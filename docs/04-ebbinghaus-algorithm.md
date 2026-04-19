# 04 — 艾宾浩斯复习算法

## 背景

艾宾浩斯（Hermann Ebbinghaus）1885 年的遗忘曲线实验表明：学习后 20 分钟能记住 58%，1 小时后 44%，1 天后 33%，1 周后 25%，1 个月后 21%。**间隔重复（Spaced Repetition）** 是基于这一曲线设计的复习策略——在将要遗忘的临界点复习，每次复习后记忆保留率提升，下次可遗忘的时间拉长。

本项目使用**固定间隔九阶段**方案，而不是 SM-2（Anki 用的那种）或 FSRS 等动态算法，理由：

1. 实现简单，易于调试和可视化
2. 用户心智模型清晰（"我现在是第几次复习"）
3. 对于词汇学习场景，固定间隔的效果已经接近动态算法
4. 未来可平滑升级到 FSRS

## 九阶段复习间隔

| 阶段 | 距上次复习的间隔 | 说明 |
|------|------------------|------|
| 1 | 初次学习（t=0） | 首次学到 |
| 2 | 5 分钟后 | 短时记忆巩固 |
| 3 | 30 分钟后 | 短时→中时转化 |
| 4 | 12 小时后 | 当日内再巩固 |
| 5 | 1 天后 | 进入长时记忆 |
| 6 | 2 天后 | 持续巩固 |
| 7 | 4 天后 | 中期巩固 |
| 8 | 7 天后 | 周级巩固 |
| 9 | 15 天后 | 半月巩固 |
| — | 30 天后 | 视为"已掌握" |

即常量数组（单位：小时）：

```
INTERVALS_HOURS = [5/60, 0.5, 12, 24, 48, 96, 168, 360, 720]
索引              0     1    2   3   4   5   6    7    8
阶段             第1→2 第2→3 ...
```

**注意**：数组有 9 个元素，对应从 stage 1 到 stage 9 的过渡间隔。stage 0 是"未学过"状态。

## 状态机

每个 `user_word_progress` 记录维护一个 `stage`（0-9），状态转移规则：

```
初始状态：stage = 0（未学过，无记录或记录 stage=0）

用户学习/复习一次：
  如果 result == correct（答对）:
    stage_new = min(stage + 1, 9)
  如果 result == wrong（答错）:
    stage_new = max(1, stage - 1)
    同时写入 wrong_word 表
  如果 result == skip（跳过）:
    stage 不变，next_review_at 延后 10 分钟

更新 last_reviewed_at = now
更新 next_review_at:
  如果 stage_new == 9 且 last_reviewed_at 已过 30 天:
    置 mastered_at = now，不再复习
  否则:
    next_review_at = last_reviewed_at + INTERVALS_HOURS[stage_new - 1] 小时
```

### 关键决策点

**Q: 为什么答错是回退 1 级而不是回到 1？**
A: 回到 1 对用户打击太大（假设用户在 stage 7 答错，回到 1 意味着前面两周的努力作废）。回退 1 级既体现了惩罚，又保留了之前的积累。

**Q: 为什么 stage 回退有下限 1？**
A: 只要学过一次就应该留在复习循环里，回到 0 会被系统当"未学过"重新调度，逻辑上不对。

**Q: stage 9 之后怎么办？**
A: 第 9 阶段（30 天）答对后，置 `mastered_at` 并停止调度。用户可在 UI 上手动触发"重新学习"（置 stage=1）。

**Q: 用户答题间隔远远超过预计的 next_review_at 怎么办？**
A: 不特殊处理。例如用户本该 2 天后复习，实际 10 天才来，算法不惩罚延迟（因为生活中总有间断）。但记得多次延迟后答错率会变高，算法通过"答错回退"自然地把 stage 拉回来。

## 核心伪代码

### 答题处理（服务端）

```python
def handle_answer(user_id, word_id, result, client_ts):
    progress = db.get_or_create_progress(user_id, word_id)
    stage_before = progress.stage
    now = server_now_utc()

    # 1. 冲突检测：如果 client_ts 比数据库里的 client_updated_at 还老，拒绝
    if progress.client_updated_at and client_ts < progress.client_updated_at:
        return conflict_response(progress)

    # 2. 状态转移
    if result == 'correct':
        stage_after = min(stage_before + 1, 9)
    elif result == 'wrong':
        stage_after = max(1, stage_before - 1)
        wrong_word_repo.upsert(user_id, word_id)
    else:  # skip
        stage_after = max(1, stage_before)  # 保证至少是 1

    # 3. 计算下次复习时间
    if stage_after >= 9:
        # 第 9 阶段完成后 30 天视为掌握
        next_review_at = now + timedelta(days=30)
    else:
        interval_hours = INTERVALS_HOURS[stage_after - 1]
        next_review_at = now + timedelta(hours=interval_hours)

    # 4. 首次学习时间 / 掌握时间记录
    if stage_before == 0 and stage_after > 0:
        progress.first_learned_at = now
    if stage_before < 9 and stage_after == 9:
        progress.mastered_at = now

    # 5. 持久化
    progress.stage = stage_after
    progress.last_reviewed_at = now
    progress.next_review_at = next_review_at
    progress.client_updated_at = client_ts
    if result == 'correct':
        progress.correct_count += 1
    elif result == 'wrong':
        progress.wrong_count += 1
    db.save(progress)

    # 6. 同时写 study_log
    study_log_repo.insert({
        'user_id': user_id, 'word_id': word_id,
        'action': 'learn' if stage_before == 0 else 'review',
        'result': result,
        'stage_before': stage_before, 'stage_after': stage_after,
        'client_ts': client_ts, 'created_at': now
    })

    return {
        'stage_before': stage_before,
        'stage_after': stage_after,
        'next_review_at': next_review_at,
        'mastered': progress.mastered_at is not None
    }
```

### 今日计划生成

```python
def get_today_plan(user_id, level_code):
    now = server_now_utc()
    settings = user_settings_repo.get(user_id)

    # 1. 到期复习的词
    due_words = progress_repo.query(
        user_id=user_id,
        level_code=level_code,
        stage__gt=0,
        stage__lt=9,
        next_review_at__lte=now,
        order_by='next_review_at ASC',
        limit=settings.daily_review_goal
    )

    # 2. 新学词
    # 找出该等级下用户还没学过的词（stage=0 或无记录）
    already_learned_ids = {p.word_id for p in progress_repo.list_all(user_id, level_code)}
    new_word_count = settings.daily_new_words_goal
    
    new_words_query = word_bank_repo.query(level_code=level_code, audit_status=1)
    new_words_candidates = [w for w in new_words_query if w.id not in already_learned_ids]
    
    # 按用户偏好排序
    if settings.default_sort_mode == 'alpha':
        new_words_candidates.sort(key=lambda w: w.word_lower)
    elif settings.default_sort_mode == 'topic':
        new_words_candidates.sort(key=lambda w: (w.topic_code or '', w.word_lower))
    elif settings.default_sort_mode == 'random':
        random.shuffle(new_words_candidates)
    
    new_words = new_words_candidates[:new_word_count]

    return {
        'review_words': enrich(due_words),   # 带上 word_bank 详情
        'new_words': new_words,
        'review_count': len(due_words),
        'new_count': len(new_words),
        'estimated_minutes': estimate_duration(len(due_words), len(new_words))
    }
```

性能提示：第 1 步走 `idx_user_next_review` 索引很快；第 2 步"未学过的词"查询如果直接 LEFT JOIN 会扫全表，推荐先拿 `progress.word_ids`（通常每用户几千个）到应用层做差集，或用"NOT EXISTS"子查询。

### 冲突解决（多设备同步）

两个设备同时离线学习同一个词，重新上线后服务端按以下规则合并：

```python
def resolve_conflict(progress_db, incoming):
    """
    progress_db: 数据库中现有记录
    incoming: 客户端上报的操作（含 stage_before/stage_after/client_ts/result）
    """
    # 规则 1：基于 client_ts 的最后写入胜出
    if progress_db.client_updated_at and incoming.client_ts <= progress_db.client_updated_at:
        # 本次上报比数据库里的记录还老，丢弃
        return 'rejected'
    
    # 规则 2：特殊情况——如果 incoming 是 correct 但数据库里比它新的记录 stage 已经更高，
    # 保留数据库里的（避免回退）
    if incoming.result == 'correct':
        expected_stage_after = min(incoming.stage_before + 1, 9)
        if progress_db.stage > expected_stage_after:
            # 数据库已经前进得比这次操作更远，说明这次操作是旧的
            return 'rejected'
    
    # 规则 3：正常应用
    apply_answer_to_progress(progress_db, incoming)
    return 'applied'
```

## 批量调度优化

对于"今日复习"的查询，如果一个用户已学了 5000 个词，扫描 `user_word_progress` 表可能涉及几千行。优化：

1. **查询裁剪**：`next_review_at` 加了索引，只查到期的那部分
2. **预取 word_bank**：把返回的 word_id 列表用 IN 查询一次性拿到词条详情
3. **Redis 缓存**：今日计划按 `user_id:date:level_code` 缓存 10 分钟

## 可视化：遗忘曲线图

前端绘制时：
- **X 轴**：时间（从首次学到现在），单位小时或天
- **Y 轴**：记忆保留率（0~1），0 是完全遗忘，1 是完全记得
- **理论曲线**：`R(t) = e^(-t/S)`，其中 S 是记忆强度，每次复习后 S 增大
- **用户实际点**：每次复习的时间 + 答对/答错（答对点在曲线上方，答错在下方）

简化画法：在每个阶段起点前画一段下降曲线（代表遗忘），用户复习节点垂直上升（代表记忆强化），然后继续下降直到下一次复习。

Java 侧不做绘制，只返回数据（见 `stats/forgetting-curve` API），前端用 ECharts / uCharts 画。

## 测试用例

### 基本流程

```
Day 1, 10:00:00  首次学习 "abandon" → stage: 0→1, next: 10:05:00
Day 1, 10:05:00  复习答对 → stage: 1→2, next: 10:35:00
Day 1, 10:35:00  复习答对 → stage: 2→3, next: 22:35:00
Day 1, 22:35:00  复习答错 → stage: 3→2, next: Day 2 10:35:00
Day 2, 10:35:00  复习答对 → stage: 2→3, next: Day 2 22:35:00
...（以此类推）
Day 16           第 9 次复习答对 → stage: 9, mastered_at 设置
```

### 边界情况

1. **stage 0 时答错**：不会发生（没学过不会被调度到复习队列）
2. **stage 1 时答错**：stage 保持 1，next_review 重新计算
3. **stage 9 时答错**：stage 回到 8，mastered_at 清空（需要回到复习循环）
4. **同一单词短时间内多次答题**：全部记录 study_log，progress 取最后一次
5. **client_ts 早于最新 client_updated_at**：拒绝
6. **client_ts 晚于服务端 now（时钟不同步）**：接受但 cap 到 server_now

## 未来扩展：FSRS

后续可切换到 FSRS（Free Spaced Repetition Scheduler）算法：

- 保留 `stage` 字段语义不变
- 新增 `difficulty`, `stability`, `retrievability` 三个字段存储 FSRS 状态
- 答题时用 FSRS 公式计算下次复习时间，`stage` 仍按固定阶段递增方便 UI 展示
- 用户可在设置中切换"经典艾宾浩斯"和"FSRS 智能模式"

接口保持不变，算法可插拔。

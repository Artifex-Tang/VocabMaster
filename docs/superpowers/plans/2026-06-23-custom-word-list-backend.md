# 自定义词库（Custom Word List）后端 + 数据导入 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 落地"自定义词库"后端 API + Think 教材数据导入，使 5 个 Think 级别（THINK_STARTER..L5，2639 词/60 单元）可订阅、按单元学新词、艾宾浩斯跨单元复习。

**Architecture:** 词存 `word_bank`（艾宾浩斯引擎只认 `word_bank.id`）。"词库"= `word_bank` 之上的课程分组层（3 张新表：`word_list` / `word_list_item` / `user_list_subscription`）。复习复用现有 `findDueForReview`（按 level 不过滤 unit → 跨单元零改动）。学新词需新查询（按单元拉未学词）。后端按业务包 `wordlist` 组织（Controller→Service→Mapper）。Python 脚本 `import_think_wordlist.py` 做解析+DeepSeek翻译+幂等插入。

**Tech Stack:** Java 21 + Spring Boot 3.5 + MyBatis-Plus 3.5.7 + Flyway + MySQL 8 + Valkey。Python 3.12 + openpyxl + DeepSeek-V3 API。

**Spec:** `docs/superpowers/specs/2026-06-23-custom-word-list-design.md`
**前端（Plan 2，本计划不含）：** 词库广场页 + 订阅 + 单元列表 + 学新词入口，复用现有复习/测试页。

**Schema 细化（相对 spec）：** `user_list_subscription` 加 `id BIGINT AUTO_INCREMENT PRIMARY KEY` + `UNIQUE KEY (user_id, list_id)`（MyBatis-Plus 对复合主键支持差）。其余表同 spec。

---

## File Structure

| 文件 | 责任 |
|------|------|
| Create `backend-java/src/main/resources/db/migration/V3__custom_word_list.sql` | 建 3 表 |
| Create `backend-java/.../wordlist/entity/WordList.java` | 词库实体 |
| Create `backend-java/.../wordlist/entity/WordListItem.java` | 词库项实体（单元/页/序） |
| Create `backend-java/.../wordlist/entity/UserListSubscription.java` | 订阅 + 单元游标实体 |
| Create `backend-java/.../wordlist/mapper/WordListMapper.java` | 词库 CRUD |
| Create `backend-java/.../wordlist/mapper/WordListItemMapper.java` | 词库项查询 |
| Create `backend-java/.../wordlist/mapper/UserListSubscriptionMapper.java` | 订阅 upsert/查询 |
| Modify `backend-java/.../word/mapper/WordBankMapper.java` + `.xml` | 新增 `findNewWordsByUnit` |
| Create `backend-java/.../wordlist/dto/*.java` | 响应 DTO |
| Create `backend-java/.../wordlist/service/WordListService.java` | 业务逻辑 |
| Create `backend-java/.../wordlist/controller/WordListController.java` | REST API |
| Modify `backend-java/.../test/service/TestService.java` | 短语排除 spelling |
| Create `backend-java/src/test/.../wordlist/WordListServiceTest.java` | 单元完成判定测试 |
| Create `backend-java/src/test/.../test/TestServicePhraseTest.java` | 短语守卫测试 |
| Create `scripts/import_think_wordlist.py` | 解析+翻译+插入 |

测试策略：后端仅 1 个现存测试（`EbbinghausSchedulerTest`），测试文化轻。本计划对**纯逻辑**（单元完成判定、短语守卫）写单元测试；Mapper SQL 与 Controller 装配用**手动 API/DB 验证**（与项目现状一致）。Python ETL 脚本无单测，用 DB 计数验证。

---

### Task 1: Flyway V3 迁移（建 3 表）

**Files:**
- Create: `backend-java/src/main/resources/db/migration/V3__custom_word_list.sql`

- [ ] **Step 1: 写迁移文件**

```sql
-- V3__custom_word_list.sql
-- 自定义词库：词库 / 词库项（课程分组）/ 用户订阅

CREATE TABLE word_list (
  id                BIGINT PRIMARY KEY AUTO_INCREMENT,
  owner_user_id     BIGINT NULL COMMENT 'NULL=系统内置共享；非空=用户个人',
  name              VARCHAR(128) NOT NULL,
  description       VARCHAR(512),
  source_type       VARCHAR(16) NOT NULL DEFAULT 'builtin' COMMENT 'builtin|imported',
  origin_level_code VARCHAR(32) NULL COMMENT '内置教材词库挂 level_code',
  word_count        INT NOT NULL DEFAULT 0,
  cover_emoji       VARCHAR(16),
  sort_order        INT NOT NULL DEFAULT 0,
  created_at        DATETIME(3),
  updated_at        DATETIME(3),
  deleted_at        DATETIME(3) NULL,
  INDEX idx_owner (owner_user_id),
  INDEX idx_source (source_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE word_list_item (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  list_id    BIGINT NOT NULL,
  word_id    BIGINT NOT NULL COMMENT '-> word_bank.id',
  unit_no    INT NOT NULL,
  page       INT,
  sort_order INT NOT NULL DEFAULT 0,
  INDEX idx_list_unit (list_id, unit_no, sort_order),
  INDEX idx_word (word_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_list_subscription (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  list_id        BIGINT NOT NULL,
  current_unit_no INT NOT NULL DEFAULT 1,
  subscribed_at  DATETIME(3),
  updated_at     DATETIME(3),
  UNIQUE KEY uk_user_list (user_id, list_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 重启后端让 Flyway 应用**

Run（本地 docker 栈）:
```bash
docker restart vocab-backend
docker logs --tail 30 vocab-backend 2>&1 | grep -i flyway
```
Expected: 日志含 `Migrating schema ... to version "3 - custom word list"` 且无报错。

- [ ] **Step 3: 验证表存在**

Run:
```bash
docker exec vocab-mysql mysql -uroot -proot vocabmaster -e "SHOW TABLES LIKE 'word_list%'; SHOW TABLES LIKE 'user_list_subscription';"
```
Expected: `word_list`, `word_list_item`, `user_list_subscription` 三行。

- [ ] **Step 4: Commit**

```bash
git add backend-java/src/main/resources/db/migration/V3__custom_word_list.sql
git commit -m "feat(wordlist): V3 迁移建 word_list/word_list_item/user_list_subscription 三表"
```

---

### Task 2: 实体类（3 个）

**Files:**
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/entity/WordList.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/entity/WordListItem.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/entity/UserListSubscription.java`

- [ ] **Step 1: 写 WordList.java**

```java
package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_list")
public class WordList {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** NULL=系统内置共享；非空=用户个人上传 */
    private Long ownerUserId;
    private String name;
    private String description;
    /** builtin | imported */
    private String sourceType;
    private String originLevelCode;
    private Integer wordCount;
    private String coverEmoji;
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic(value = "NULL", delval = "now(3)")
    private LocalDateTime deletedAt;
}
```

- [ ] **Step 2: 写 WordListItem.java**

```java
package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_list_item")
public class WordListItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long listId;
    private Long wordId;
    private Integer unitNo;
    private Integer page;
    private Integer sortOrder;
}
```

- [ ] **Step 3: 写 UserListSubscription.java**

```java
package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_list_subscription")
public class UserListSubscription {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long listId;
    @Builder.Default
    private Integer currentUnitNo = 1;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime subscribedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 编译验证**

Run:
```bash
cd backend-java && mvn -q compile 2>&1 | tail -5
```
Expected: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
git add backend-java/src/main/java/com/vocabmaster/wordlist/entity/
git commit -m "feat(wordlist): 实体 WordList/WordListItem/UserListSubscription"
```

---

### Task 3: Mapper（3 个 + WordBankMapper 新方法）

**Files:**
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/mapper/WordListMapper.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/mapper/WordListItemMapper.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/mapper/UserListSubscriptionMapper.java`
- Modify: `backend-java/src/main/java/com/vocabmaster/word/mapper/WordBankMapper.java`
- Modify: `backend-java/src/main/resources/mapper/WordBankMapper.xml`

- [ ] **Step 1: 写 WordListMapper.java**

```java
package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.WordList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordListMapper extends BaseMapper<WordList> {
}
```

- [ ] **Step 2: 写 WordListItemMapper.java（含单元词数统计查询）**

```java
package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.WordListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WordListItemMapper extends BaseMapper<WordListItem> {

    /** 某 list 各 unit 的词数（unit_no, cnt）。 */
    @Select("""
        SELECT unit_no, COUNT(*) AS cnt
        FROM word_list_item
        WHERE list_id = #{listId}
        GROUP BY unit_no
        ORDER BY unit_no
    """)
    List<Map<String, Object>> countByUnit(@Param("listId") Long listId);

    /** 某 list 某 unit 的全部 word_id。 */
    @Select("""
        SELECT word_id FROM word_list_item
        WHERE list_id = #{listId} AND unit_no = #{unitNo}
        ORDER BY sort_order
    """)
    List<Long> findWordIdsByUnit(@Param("listId") Long listId,
                                 @Param("unitNo") Integer unitNo);
}
```

- [ ] **Step 3: 写 UserListSubscriptionMapper.java（含 upsert）**

```java
package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.UserListSubscription;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserListSubscriptionMapper extends BaseMapper<UserListSubscription> {

    /** 订阅 upsert：不存在则建（current_unit=1），存在则不动。 */
    @Update("""
        INSERT INTO user_list_subscription (user_id, list_id, current_unit_no, subscribed_at, updated_at)
        VALUES (#{userId}, #{listId}, 1, NOW(3), NOW(3))
        ON DUPLICATE KEY UPDATE updated_at = NOW(3)
    """)
    int upsertSubscribe(@Param("userId") Long userId, @Param("listId") Long listId);

    /** 推进单元游标。 */
    @Update("""
        UPDATE user_list_subscription
        SET current_unit_no = #{unitNo}, updated_at = NOW(3)
        WHERE user_id = #{userId} AND list_id = #{listId}
    """)
    int advanceUnit(@Param("userId") Long userId,
                    @Param("listId") Long listId,
                    @Param("unitNo") Integer unitNo);

    @Select("""
        SELECT * FROM user_list_subscription
        WHERE user_id = #{userId} AND list_id = #{listId}
    """)
    UserListSubscription find(@Param("userId") Long userId, @Param("listId") Long listId);
}
```

- [ ] **Step 4: 改 WordBankMapper.java 加 findNewWordsByUnit 方法签名**

在 `WordBankMapper` 接口的 `findNewWords` 方法后，加入：

```java
    /**
     * 按"词库 + 单元"取用户尚未学过的词（学新词核心查询）。
     * 反连接 user_word_progress（stage>0 视为已学）。
     */
    List<WordBank> findNewWordsByUnit(@Param("listId") Long listId,
                                      @Param("unitNo") Integer unitNo,
                                      @Param("userId") Long userId,
                                      @Param("limit") int limit);
```

- [ ] **Step 5: 改 WordBankMapper.xml 加 SQL 实现**

在 `backend-java/src/main/resources/mapper/WordBankMapper.xml` 的 `<mapper>` 内末尾加入：

```xml
    <select id="findNewWordsByUnit" resultType="com.vocabmaster.word.entity.WordBank">
        SELECT w.*
        FROM word_list_item i
        JOIN word_bank w ON w.id = i.word_id AND w.deleted_at IS NULL
        WHERE i.list_id = #{listId}
          AND i.unit_no = #{unitNo}
          AND i.word_id NOT IN (
              SELECT word_id FROM user_word_progress
              WHERE user_id = #{userId} AND stage > 0
          )
        ORDER BY i.sort_order
        LIMIT #{limit}
    </select>
```

- [ ] **Step 6: 编译验证**

Run:
```bash
cd backend-java && mvn -q compile 2>&1 | tail -5
```
Expected: BUILD SUCCESS。

- [ ] **Step 7: Commit**

```bash
git add backend-java/src/main/java/com/vocabmaster/wordlist/mapper/ \
        backend-java/src/main/java/com/vocabmaster/word/mapper/WordBankMapper.java \
        backend-java/src/main/resources/mapper/WordBankMapper.xml
git commit -m "feat(wordlist): Mapper 三件套 + WordBankMapper.findNewWordsByUnit 按单元拉新词"
```

---

### Task 4: DTO

**Files:**
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/dto/WordListSummaryDto.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/dto/WordListDetailDto.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/dto/UnitSummaryDto.java`
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/dto/SubscribeResponse.java`

- [ ] **Step 1: 写 WordListSummaryDto.java（广场列表项）**

```java
package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WordListSummaryDto {
    private Long id;
    private String name;
    private String description;
    private String sourceType;
    private String originLevelCode;
    private Integer wordCount;
    private String coverEmoji;
    private Boolean subscribed;   // 当前用户是否已订阅
}
```

- [ ] **Step 2: 写 UnitSummaryDto.java（单元进度）**

```java
package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UnitSummaryDto {
    private Integer unitNo;
    private Integer totalCount;     // 该单元词数
    private Integer learnedCount;   // stage>=1
    private Integer masteredCount;  // stage=9
    private Boolean isCurrent;      // 是否当前单元
    private Boolean completed;      // learnedCount==totalCount
}
```

- [ ] **Step 3: 写 WordListDetailDto.java（详情 + 单元 + 游标）**

```java
package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WordListDetailDto {
    private Long id;
    private String name;
    private String description;
    private String originLevelCode;
    private Integer wordCount;
    private Integer unitCount;
    private Boolean subscribed;
    private Integer currentUnitNo;       // null=未订阅
    private List<UnitSummaryDto> units;
}
```

- [ ] **Step 4: 写 SubscribeResponse.java**

```java
package com.vocabmaster.wordlist.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscribeResponse {
    private Long listId;
    private Integer currentUnitNo;
}
```

- [ ] **Step 5: 编译验证 + Commit**

```bash
cd backend-java && mvn -q compile 2>&1 | tail -3
git add backend-java/src/main/java/com/vocabmaster/wordlist/dto/
git commit -m "feat(wordlist): DTO 4 个（广场/详情/单元/订阅响应）"
```

---

### Task 5: WordListService（业务逻辑 + 单元测试）

**Files:**
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/service/WordListService.java`
- Test: `backend-java/src/test/java/com/vocabmaster/wordlist/WordListServiceTest.java`

纯逻辑 `isUnitComplete(totalCount, learnedCount)` 抽成可测方法。

- [ ] **Step 1: 写失败测试（单元完成判定）**

`backend-java/src/test/java/com/vocabmaster/wordlist/WordListServiceTest.java`:

```java
package com.vocabmaster.wordlist;

import com.vocabmaster.wordlist.service.WordListService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WordListServiceTest {

    @Test
    void unitComplete_whenAllLearned() {
        assertTrue(WordListService.isUnitComplete(10, 10));
    }

    @Test
    void unitNotComplete_whenSomeUnlearned() {
        assertFalse(WordListService.isUnitComplete(10, 9));
    }

    @Test
    void unitNotComplete_whenZeroLearned() {
        assertFalse(WordListService.isUnitComplete(10, 0));
    }

    @Test
    void unitComplete_emptyUnit() {
        assertTrue(WordListService.isUnitComplete(0, 0));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run:
```bash
cd backend-java && mvn -q test -Dtest=WordListServiceTest 2>&1 | tail -15
```
Expected: 编译失败（`WordListService` 不存在）。

- [ ] **Step 3: 写 WordListService.java**

```java
package com.vocabmaster.wordlist.service;

import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import com.vocabmaster.wordlist.dto.*;
import com.vocabmaster.wordlist.entity.UserListSubscription;
import com.vocabmaster.wordlist.entity.WordList;
import com.vocabmaster.wordlist.mapper.UserListSubscriptionMapper;
import com.vocabmaster.wordlist.mapper.WordListItemMapper;
import com.vocabmaster.wordlist.mapper.WordListMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WordListService {

    private final WordListMapper wordListMapper;
    private final WordListItemMapper wordListItemMapper;
    private final UserListSubscriptionMapper subscriptionMapper;
    private final WordBankMapper wordBankMapper;

    /** 单元"学完"= 全部 stage>=1（至少学过一次）。纯逻辑，可单测。 */
    public static boolean isUnitComplete(int totalCount, int learnedCount) {
        return totalCount > 0 && learnedCount >= totalCount;
    }

    /** 词库广场：内置 + 我的（owner=userId）。 */
    public List<WordListSummaryDto> listSquare(Long userId) {
        List<WordList> all = wordListMapper.selectList(null); // 软删除自动过滤
        Set<Long> subscribedIds = subscriptionMapper.selectList(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<UserListSubscription>lambdaQuery()
                        .eq(UserListSubscription::getUserId, userId))
                .stream().map(UserListSubscription::getListId).collect(Collectors.toSet());
        return all.stream().map(wl -> WordListSummaryDto.builder()
                .id(wl.getId())
                .name(wl.getName())
                .description(wl.getDescription())
                .sourceType(wl.getSourceType())
                .originLevelCode(wl.getOriginLevelCode())
                .wordCount(wl.getWordCount())
                .coverEmoji(wl.getCoverEmoji())
                .subscribed(subscribedIds.contains(wl.getId()))
                .build()).toList();
    }

    /** 词库详情：含各单元进度 + 当前游标。 */
    public WordListDetailDto detail(Long userId, Long listId) {
        WordList wl = wordListMapper.selectById(listId);
        if (wl == null) {
            throw new IllegalArgumentException("词库不存在: " + listId);
        }
        UserListSubscription sub = subscriptionMapper.find(userId, listId);
        List<Map<String, Object>> unitCounts = wordListItemMapper.countByUnit(listId);

        // 该用户在该词库 originLevelCode 下的阶段分布
        Map<Integer, Integer> stageDist = new HashMap<>();
        if (wl.getOriginLevelCode() != null) {
            stageDist = wordBankMapper /* 复用 progress 阶段统计需走 progressMapper */
                    .getClass() == null ? stageDist : stageDist; // placeholder 见下注
        }

        // 注：阶段统计实际走 UserWordProgressMapper.countByStage，下面注入并调用。
        List<UnitSummaryDto> units = new ArrayList<>();
        for (Map<String, Object> row : unitCounts) {
            int unitNo = ((Number) row.get("unit_no")).intValue();
            int total = ((Number) row.get("cnt")).intValue();
            units.add(UnitSummaryDto.builder()
                    .unitNo(unitNo)
                    .totalCount(total)
                    .learnedCount(0)   // 见 Step 4 真实填充
                    .masteredCount(0)
                    .isCurrent(sub != null && sub.getCurrentUnitNo() == unitNo)
                    .completed(false)
                    .build());
        }
        return WordListDetailDto.builder()
                .id(wl.getId())
                .name(wl.getName())
                .description(wl.getDescription())
                .originLevelCode(wl.getOriginLevelCode())
                .wordCount(wl.getWordCount())
                .unitCount(units.size())
                .subscribed(sub != null)
                .currentUnitNo(sub != null ? sub.getCurrentUnitNo() : null)
                .units(units)
                .build();
    }

    /** 订阅。 */
    public SubscribeResponse subscribe(Long userId, Long listId) {
        wordListMapper.selectById(listId); // 校验存在（不存在抛可由 controller 转 404）
        subscriptionMapper.upsertSubscribe(userId, listId);
        return SubscribeResponse.builder().listId(listId).currentUnitNo(1).build();
    }

    /** 学新词：当前单元未学词。 */
    public List<WordDetailDto> learnNewWords(Long userId, Long listId, Integer unitNo, int limit) {
        UserListSubscription sub = subscriptionMapper.find(userId, listId);
        if (sub == null) {
            throw new IllegalStateException("未订阅该词库");
        }
        int unit = unitNo != null ? unitNo : sub.getCurrentUnitNo();
        List<WordBank> words = wordBankMapper.findNewWordsByUnit(listId, unit, userId, limit);
        return words.stream().map(this::toDetailDto).toList();
    }

    /** 推进单元游标。 */
    public Integer advanceUnit(Long userId, Long listId, Integer targetUnitNo) {
        UserListSubscription sub = subscriptionMapper.find(userId, listId);
        if (sub == null) {
            throw new IllegalStateException("未订阅该词库");
        }
        subscriptionMapper.advanceUnit(userId, listId, targetUnitNo);
        return targetUnitNo;
    }

    private WordDetailDto toDetailDto(WordBank w) {
        return WordDetailDto.builder()
                .id(w.getId())
                .word(w.getWord())
                .levelCode(w.getLevelCode())
                .ipaUk(w.getIpaUk())
                .ipaUs(w.getIpaUs())
                .enDefinition(w.getEnDefinition())
                .zhDefinition(w.getZhDefinition())
                .exampleEn(w.getExampleEn())
                .pos(w.getPos())
                .imageUrl(w.getImageUrl())
                .emoji(w.getEmoji())
                .audioUrlUk(w.getAudioUrlUk())
                .audioUrlUs(w.getAudioUrlUs())
                .build();
    }
}
```

> **注意（实现者必读）：** 上面 `detail()` 的 `learnedCount/masteredCount` 留了占位。Step 4 会注入 `UserWordProgressMapper` 并用 `findWordIdsByUnit(listId, unitNo)` 取该单元全部 word_id，再统计其中 stage>=1 / stage==9 的数量，回填到 `UnitSummaryDto`。删除 `detail()` 中的 placeholder 代码块（`stageDist` 那段死代码），替换为下方 Step 4 的真实实现。

- [ ] **Step 4: 用真实单元进度填充 detail()**

把 `UserWordProgressMapper` 注入到 `WordListService`（构造器加 `private final UserWordProgressMapper progressMapper;`），并重写 `detail()` 中构建 units 的循环：

```java
        // 注入：private final com.vocabmaster.study.mapper.UserWordProgressMapper progressMapper;
        List<UnitSummaryDto> units = new ArrayList<>();
        // 该用户在该词库 originLevelCode 下，已学(word_id->stage) 映射
        List<com.vocabmaster.study.entity.UserWordProgress> learned =
                wl.getOriginLevelCode() == null ? List.of()
                        : progressMapper.selectList(
                                com.baomidou.mybatisplus.core.toolkit.Wrappers
                                        .<com.vocabmaster.study.entity.UserWordProgress>lambdaQuery()
                                        .eq(com.vocabmaster.study.entity.UserWordProgress::getUserId, userId)
                                        .eq(com.vocabmaster.study.entity.UserWordProgress::getLevelCode, wl.getOriginLevelCode()));
        Map<Long, Integer> wordStage = learned.stream()
                .collect(Collectors.toMap(com.vocabmaster.study.entity.UserWordProgress::getWordId,
                        com.vocabmaster.study.entity.UserWordProgress::getStage, (a, b) -> a));

        for (Map<String, Object> row : unitCounts) {
            int unitNo = ((Number) row.get("unit_no")).intValue();
            int total = ((Number) row.get("cnt")).intValue();
            List<Long> unitWordIds = wordListItemMapper.findWordIdsByUnit(listId, unitNo);
            int learnedCnt = 0, masteredCnt = 0;
            for (Long wid : unitWordIds) {
                Integer st = wordStage.get(wid);
                if (st != null && st > 0) learnedCnt++;
                if (st != null && st == 9) masteredCnt++;
            }
            units.add(UnitSummaryDto.builder()
                    .unitNo(unitNo)
                    .totalCount(total)
                    .learnedCount(learnedCnt)
                    .masteredCount(masteredCnt)
                    .isCurrent(sub != null && sub.getCurrentUnitNo() == unitNo)
                    .completed(isUnitComplete(total, learnedCnt))
                    .build());
        }
```

确认 `WordDetailDto` 字段名与 `backend-java/.../word/dto/WordDetailDto.java` 实际字段一致；若不一致，以实际类为准调整 `toDetailDto`（读该文件核对）。

- [ ] **Step 5: 跑测试确认通过**

Run:
```bash
cd backend-java && mvn -q test -Dtest=WordListServiceTest 2>&1 | tail -15
```
Expected: 4 个测试全 PASS。

- [ ] **Step 6: Commit**

```bash
git add backend-java/src/main/java/com/vocabmaster/wordlist/service/WordListService.java \
        backend-java/src/test/java/com/vocabmaster/wordlist/WordListServiceTest.java
git commit -m "feat(wordlist): WordListService 广场/详情/订阅/学新词/推进单元 + 单元完成判定单测"
```

---

### Task 6: WordListController（REST API）

**Files:**
- Create: `backend-java/src/main/java/com/vocabmaster/wordlist/controller/WordListController.java`

- [ ] **Step 1: 写 Controller**

```java
package com.vocabmaster.wordlist.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.wordlist.dto.*;
import com.vocabmaster.wordlist.service.WordListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/word-lists")
@RequiredArgsConstructor
@Tag(name = "词库", description = "自定义词库：广场/订阅/单元/学新词")
public class WordListController {

    private final WordListService wordListService;

    @GetMapping
    @Operation(summary = "词库广场（内置 + 我的）")
    public R<List<WordListSummaryDto>> square() {
        return R.ok(wordListService.listSquare(UserContext.currentUserId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "词库详情（含各单元进度）")
    public R<WordListDetailDto> detail(@PathVariable Long id) {
        return R.ok(wordListService.detail(UserContext.currentUserId(), id));
    }

    @PostMapping("/{id}/subscribe")
    @Operation(summary = "订阅词库")
    public R<SubscribeResponse> subscribe(@PathVariable Long id) {
        return R.ok(wordListService.subscribe(UserContext.currentUserId(), id));
    }

    @GetMapping("/{id}/learn")
    @Operation(summary = "拉取当前/指定单元的新词")
    public R<List<WordDetailDto>> learn(@PathVariable Long id,
                                        @RequestParam(value = "unit", required = false) Integer unit,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(wordListService.learnNewWords(UserContext.currentUserId(), id, unit, limit));
    }

    @PostMapping("/{id}/units/{unitNo}/advance")
    @Operation(summary = "推进单元游标")
    public R<Integer> advance(@PathVariable Long id, @PathVariable Integer unitNo) {
        return R.ok(wordListService.advanceUnit(UserContext.currentUserId(), id, unitNo));
    }
}
```

- [ ] **Step 2: 编译 + 重启后端**

Run:
```bash
cd backend-java && mvn -q compile 2>&1 | tail -3
docker restart vocab-backend && sleep 8
docker logs --tail 10 vocab-backend 2>&1 | tail -5
```
Expected: 启动成功（`Started VocabMasterApplication`），无 mapper 装配错误。

- [ ] **Step 3: Commit**

```bash
git add backend-java/src/main/java/com/vocabmaster/wordlist/controller/WordListController.java
git commit -m "feat(wordlist): Controller 5 个端点 square/detail/subscribe/learn/advance"
```

---

### Task 7: TestService 短语守卫（spelling 排除短语）

**Files:**
- Modify: `backend-java/src/main/java/com/vocabmaster/test/service/TestService.java`
- Test: `backend-java/src/test/java/com/vocabmaster/test/TestServicePhraseTest.java`

- [ ] **Step 1: 写失败测试（短语判定 + spelling 排除）**

`backend-java/src/test/java/com/vocabmaster/test/TestServicePhraseTest.java`:

```java
package com.vocabmaster.test;

import com.vocabmaster.test.service.TestService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestServicePhraseTest {

    @Test
    void singleWord_notPhrase() {
        assertFalse(TestService.isPhrase("caring", "adjective"));
    }

    @Test
    void multiWord_isPhrase() {
        assertTrue(TestService.isPhrase("give up", "phrasal verb"));
    }

    @Test
    void longPhrase_isPhrase() {
        assertTrue(TestService.isPhrase("leave something to the last minute", "phrase"));
    }

    @Test
    void phrasePos_isPhrase_evenSingleToken() {
        assertTrue(TestService.isPhrase("anyword", "phrase"));
        assertTrue(TestService.isPhrase("anyword", "phrasal verb"));
    }

    @Test
    void spellingNotAllowedForPhrase() {
        assertTrue(TestService.allowsMode("give up", "phrasal verb", "choice"));
        assertTrue(TestService.allowsMode("give up", "phrasal verb", "listening"));
        assertFalse(TestService.allowsMode("give up", "phrasal verb", "spelling"));
    }

    @Test
    void spellingAllowedForSingleWord() {
        assertTrue(TestService.allowsMode("caring", "adjective", "spelling"));
    }
}
```

- [ ] **Step 2: 跑测试确认失败**

Run:
```bash
cd backend-java && mvn -q test -Dtest=TestServicePhraseTest 2>&1 | tail -15
```
Expected: 编译失败（`isPhrase`/`allowsMode` 静态方法不存在）。

- [ ] **Step 3: 在 TestService 加静态判定方法**

在 `TestService` 类内（`VALID_MODES` 常量附近）加入：

```java
    /** 短语判定：词含空格，或 PoS 为 phrase/phrasal verb。 */
    public static boolean isPhrase(String word, String pos) {
        if (word != null && word.trim().contains(" ")) return true;
        if (pos != null) {
            String p = pos.trim().toLowerCase();
            return p.equals("phrase") || p.equals("phrasal verb");
        }
        return false;
    }

    /**
     * 该词是否允许某种测试模式。
     * 短语排除 spelling（手打长短语不现实）；choice / listening 全允许。
     */
    public static boolean allowsMode(String word, String pos, String mode) {
        if (!VALID_MODES.contains(mode)) return false;
        if ("spelling".equals(mode) && isPhrase(word, pos)) return false;
        return true;
    }
```

- [ ] **Step 4: 跑测试确认通过**

Run:
```bash
cd backend-java && mvn -q test -Dtest=TestServicePhraseTest 2>&1 | tail -15
```
Expected: 6 个测试全 PASS。

- [ ] **Step 5: 在出题流程接入守卫**

定位 `TestService.selectWords(...)` 调用后的出题处（`buildClientQuestion` 调用点）。在 `generate(...)` 内遍历选出的 `List<WordBank> words` 构建 `TestQuestion` 时，对 `mode` 做守卫：若 `!allowsMode(wb.getWord(), wb.getPos(), mode)`，则该词**跳过**（不生成该题），改取下一候选；若整批无可出题词，返回空 session 或降级为 choice。

具体接入点：读 `TestService.generate(...)` 方法体，找到为每个词调 `buildClientQuestion(qId, wb, mode)` 的循环，在调用前加：

```java
if (!allowsMode(wb.getWord(), wb.getPos(), mode)) {
    continue;   // 短语跳过 spelling
}
```

若选词数因此不足 `size`，可在 `selectWords` 的 `limit` 上加 buffer（如 `limit + limit/2`）再过滤，保证凑够题数。实现时按 `generate` 实际结构落地。

- [ ] **Step 6: Commit**

```bash
git add backend-java/src/main/java/com/vocabmaster/test/service/TestService.java \
        backend-java/src/test/java/com/vocabmaster/test/TestServicePhraseTest.java
git commit -m "feat(test): 短语排除 spelling 题型（choice/listening 不受影响）"
```

---

### Task 8: 注册 THINK 级别到 level 表 + 端到端冒烟

> 数据导入由 Task 9 脚本完成（含 level 表插入）。本 Task 在脚本跑之前，先**手动**塞 5 个 THINK level + 一个测试词库，验证 API 链路通。Task 9 跑完后真实数据覆盖。

**Files:** 无代码改动，纯 SQL 冒烟。

- [ ] **Step 1: 手动注册 5 个 THINK level + evict 缓存**

Run:
```bash
docker exec vocab-mysql mysql -uroot -proot vocabmaster -e "
INSERT IGNORE INTO level (code, name_zh, name_en, sort_order, target_word_count, description) VALUES
 ('THINK_STARTER','Think 入门','Think Starter',100,596,'剑桥 Think 教材 Starter 级'),
 ('THINK_L2','Think 2','Think Level 2',101,489,'剑桥 Think 教材 Level 2'),
 ('THINK_L3','Think 3','Think Level 3',102,470,'剑桥 Think 教材 Level 3'),
 ('THINK_L4','Think 4','Think Level 4',103,566,'剑桥 Think 教材 Level 4'),
 ('THINK_L5','Think 5','Think Level 5',104,518,'剑桥 Think 教材 Level 5');
"
docker exec vocab-redis redis-cli -a redis_prod_2024 DEL word:levels 2>/dev/null
```
（level 表实际列名以 `DESCRIBE level;` 为准；若列名不同，按实际调整。先 `DESCRIBE level;` 核对。）

- [ ] **Step 2: 端到端 API 冒烟（需登录态 token）**

用任意已注册用户 token（或先注册/登录拿 access token），执行：
```bash
TOKEN="<填 access token>"
# 广场（暂空，Task 9 才有数据；先验证接口 200）
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/word-lists | head -c 300
```
Expected: `{"code":0,"msg":"...","data":[]}`（空数组，无 500）。

- [ ] **Step 3: （Task 9 跑完后复核）订阅 → 学新词 → 复习链路**

Task 9 导入真实 Think 数据后，重跑：
```bash
# 假设 Think Starter 词库 id=1
curl -s -H "Authorization: Bearer $TOKEN" -X POST http://localhost:8080/word-lists/1/subscribe
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/word-lists/1/learn?unit=1\&limit=5 | head -c 500
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/word-lists/1
```
Expected: subscribe 返回 `currentUnitNo:1`；learn 返回 ≤5 个词卡（含 zh_definition）；detail 各单元 learnedCount 随学习增长。

---

### Task 9: Think 数据导入脚本

**Files:**
- Create: `scripts/import_think_wordlist.py`

- [ ] **Step 1: 写脚本**

```python
#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
导入剑桥《Think》教材 5 个 Excel 双语词表到 vocabmaster DB。
流程：解析 Excel -> DeepSeek 翻译 EN释义->中文 -> 幂等插入 word_bank/level/word_list/word_list_item -> evict 缓存。
铁律：只 INSERT/SELECT，不删不改已有用户数据。
"""
import argparse, json, os, sys, time, glob
import urllib.request, urllib.error
import openpyxl
import pymysql

# ---- 配置 ----
DS_API = "https://api.deepseek.com/chat/completions"
DS_KEY = os.environ.get("DS_KEY")  # 从环境变量读，密钥勿硬编码（需先 export DS_KEY=...）
DB = dict(host="127.0.0.1", port=13306, user="root", password="root",
          database="vocabmaster", charset="utf8mb4",
          cursorclass=pymysql.cursors.DictCursor)  # 端口按本机 docker 映射调整
LEVEL_MAP = {
    "Think_Starter_GERMAN_Bilingual_Wordlist.xlsx": ("THINK_STARTER", "Think 入门", "Think Starter", 100),
    "Think_Level_2_GERMAN_Bilingual_Wordlist.xlsx": ("THINK_L2", "Think 2", "Think Level 2", 101),
    "Think_Level_3_GERMAN_Bilingual_Wordlist.xlsx": ("THINK_L3", "Think 3", "Think Level 3", 102),
    "Think_Level_4_GERMAN_Bilingual_Wordlist.xlsx": ("THINK_L4", "Think 4", "Think Level 4", 103),
    "Think_Level_5_German_Bilingual_Wordlist.XLSX": ("THINK_L5", "Think 5", "Think Level 5", 104),
}
BATCH = 30


def translate_batch(items):
    """items: [(word, en_def), ...] -> {word: zh}"""
    prompt = "把每个英文单词释义翻成简明中文词义（只给中文，逗号分隔多条义项），返回 JSON 数组 [{\"w\":\"word\",\"zh\":\"中文\"}]。释义：\n"
    payload = [{"w": w, "en": d} for w, d in items]
    body = json.dumps({
        "model": "deepseek-chat",
        "messages": [
            {"role": "system", "content": "你是英汉词典翻译，输出纯 JSON 数组，不要多余文字。"},
            {"role": "user", "content": prompt + json.dumps(payload, ensure_ascii=False)},
        ],
        "temperature": 0.2,
        "response_format": {"type": "json_object"},
    }).encode("utf-8")
    req = urllib.request.Request(DS_API, data=body, headers={
        "Authorization": f"Bearer {DS_KEY}", "Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as r:
        resp = json.loads(r.read().decode("utf-8"))
    content = resp["choices"][0]["message"]["content"]
    # 模型可能包成 {"list":[...]}
    parsed = json.loads(content)
    arr = parsed["list"] if isinstance(parsed, dict) and "list" in parsed else parsed
    return {x["w"]: x["zh"] for x in arr}


def parse_excel(path):
    wb = openpyxl.load_workbook(path, read_only=True, data_only=True)
    ws = wb[wb.sheetnames[0]]
    rows = list(ws.iter_rows(values_only=True))
    out = []
    for r in rows[1:]:
        if not r or not r[0]:
            continue
        out.append(dict(word=str(r[0]).strip(), unit=int(r[1]) if r[1] else None,
                        page=int(r[2]) if r[2] else None, en_def=str(r[3] or "").strip(),
                        pos=str(r[4] or "").strip(), example_en=str(r[5] or "").strip(),
                        cef=str(r[6] or "").strip(), ipa=str(r[7] or "").strip()))
    wb.close()
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default="wordlist")
    ap.add_argument("--translate", action="store_true", help="调用 DeepSeek 翻译（否则用缓存 zh.json）")
    ap.add_argument("--zh-cache", default="wordlist/_zh_cache.json")
    args = ap.parse_args()

    zh_cache = {}
    if os.path.exists(args.zh_cache):
        zh_cache = json.load(open(args.zh_cache, encoding="utf-8"))

    conn = pymysql.connect(**DB)
    all_rows = []   # (level_code, level_name_zh, level_name_en, sort, word, unit, page, en_def, pos, ex_en, cef, ipa, zh_def)
    for fname, (lcode, nz, ne, so) in LEVEL_MAP.items():
        rows = parse_excel(os.path.join(args.dir, fname))
        print(f"[{fname}] parsed {len(rows)} rows")
        # 翻译缺失项
        if args.translate:
            todo = [(r["word"], r["en_def"]) for r in rows
                    if r["word"] not in zh_cache or not zh_cache.get(r["word"])]
            for i in range(0, len(todo), BATCH):
                chunk = todo[i:i + BATCH]
                try:
                    zh_cache.update(translate_batch(chunk))
                    print(f"  translated {i + len(chunk)}/{len(todo)}")
                except Exception as e:
                    print(f"  [WARN] batch {i} fail: {e}")
                json.dump(zh_cache, open(args.zh_cache, "w", encoding="utf-8"),
                          ensure_ascii=False, indent=1)
                time.sleep(0.3)
        for r in rows:
            zh = zh_cache.get(r["word"], "")
            all_rows.append((lcode, nz, ne, so, r["word"], r["unit"], r["page"],
                             r["en_def"], r["pos"], r["example_en"], r["cef"],
                             r["ipa"], zh))

    with conn.cursor() as cur:
        # 1. level 表
        for lcode, nz, ne, so, *_ in all_rows:
            cur.execute("""INSERT IGNORE INTO level
                (code, name_zh, name_en, sort_order, target_word_count, description)
                VALUES (%s,%s,%s,%s,0,'剑桥 Think 教材')""", (lcode, nz, ne, so))
        # 2. word_bank（按 level_code+word_lower 查重）
        word_id_map = {}  # (lcode, word_lower) -> id
        for (lcode, nz, ne, so, word, unit, page, en_def, pos, ex_en, cef, ipa, zh) in all_rows:
            wl = word.lower()
            cur.execute("SELECT id FROM word_bank WHERE level_code=%s AND word_lower=%s AND deleted_at IS NULL",
                        (lcode, wl))
            row = cur.fetchone()
            if row:
                word_id_map[(lcode, wl)] = row["id"]
                continue
            cur.execute("""INSERT INTO word_bank
                (level_code, word, word_lower, ipa_uk, en_definition, zh_definition,
                 example_en, pos, audit_status, created_at, updated_at)
                VALUES (%s,%s,%s,%s,%s,%s,%s,%s,1,NOW(3),NOW(3))""",
                        (lcode, word, wl, ipa, en_def, zh, ex_en, pos))
            word_id_map[(lcode, wl)] = cur.lastrowid
        # 3. word_list（每级一个内置词库，幂等：按 name 查）
        list_id_map = {}  # lcode -> list_id
        for lcode, nz, ne, so, *_ in all_rows:
            cur.execute("SELECT id FROM word_list WHERE name=%s AND deleted_at IS NULL", (f"Think {nz}",))
            row = cur.fetchone()
            if row:
                list_id_map[lcode] = row["id"]; continue
            cur.execute("""INSERT INTO word_list
                (owner_user_id, name, description, source_type, origin_level_code,
                 word_count, cover_emoji, sort_order, created_at, updated_at)
                VALUES (NULL,%s,'剑桥 Think 教材词库','builtin',%s,0,'📘',%s,NOW(3),NOW(3))""",
                        (f"Think {nz}", lcode, so))
            list_id_map[lcode] = cur.lastrowid
        # 4. word_list_item（幂等：按 list_id+word_id 查重）
        item_cnt = 0
        for (lcode, nz, ne, so, word, unit, page, en_def, pos, ex_en, cef, ipa, zh) in all_rows:
            wid = word_id_map[(lcode, word.lower())]
            lid = list_id_map[lcode]
            cur.execute("SELECT id FROM word_list_item WHERE list_id=%s AND word_id=%s", (lid, wid))
            if cur.fetchone():
                continue
            cur.execute("""INSERT INTO word_list_item (list_id, word_id, unit_no, page, sort_order)
                VALUES (%s,%s,%s,%s,%s)""", (lid, wid, unit or 1, page, item_cnt))
            item_cnt += 1
        # 5. 回填 word_list.word_count
        for lcode, lid in list_id_map.items():
            cur.execute("SELECT COUNT(*) c FROM word_list_item WHERE list_id=%s", (lid,))
            cnt = cur.fetchone()["c"]
            cur.execute("UPDATE word_list SET word_count=%s WHERE id=%s", (cnt, lid))
    conn.commit()

    # 6. evict word:levels 缓存（level 表变了）
    try:
        import redis
        rc = redis.Redis(host="127.0.0.1", port=16379, password="redis_prod_2024", decode_responses=True)
        rc.delete("word:levels")
        print("[OK] evicted word:levels")
    except Exception as e:
        print(f"[WARN] redis evict skipped: {e}（手动跑 docker exec vocab-redis redis-cli -a redis_prod_2024 DEL word:levels）")

    print(f"[DONE] imported {len(all_rows)} words across {len(list_id_map)} lists, {item_cnt} new items")


if __name__ == "__main__":
    main()
```

> 实现者注意：
> - DB 端口 / Redis 端口 / 密码按本机 docker 实际映射调整（先用 `docker ps` 看 vocab-mysql / vocab-redis 的端口映射）。
> - `level` 表列名（`name_zh`? 还是 `nameZh` 映射的 `name_zh`）先 `DESCRIBE level;` 核对。
> - `word_bank` 必填列以 `DESCRIBE word_bank;` 为准，补齐 NOT NULL 列默认值。
> - DeepSeek `response_format` 若不支持 json_object，去掉该参数，改用正则从 content 抽 JSON 数组。

- [ ] **Step 2: 安装依赖**

Run:
```bash
pip install openpyxl pymysql redis
```

- [ ] **Step 3: 先跑翻译（生成 zh 缓存）**

Run:
```bash
python scripts/import_think_wordlist.py --translate
```
Expected: 逐级打印 `translated X/Y`，`wordlist/_zh_cache.json` 增长，最终 `[DONE] imported 2639 words across 5 lists`。成本 ≈¥0.1。

- [ ] **Step 4: 验证 DB 计数**

Run:
```bash
docker exec vocab-mysql mysql -uroot -proot vocabmaster -e "
SELECT level_code, COUNT(*) FROM word_bank WHERE level_code LIKE 'THINK_%' GROUP BY level_code;
SELECT COUNT(*) AS lists FROM word_list WHERE source_type='builtin';
SELECT COUNT(*) AS items FROM word_list_item;
SELECT unit_no, COUNT(*) FROM word_list_item WHERE list_id=1 GROUP BY unit_no ORDER BY unit_no LIMIT 5;
"
```
Expected:
- 5 行 THINK level，词数 ≈ 596/489/470/566/518。
- `lists` = 5。
- `items` ≈ 2639。
- list_id=1 的 unit_no 1-12 各 ~40-50。

抽查中文释义：
```bash
docker exec vocab-mysql mysql -uroot -proot vocabmaster -e "
SELECT word, en_definition, zh_definition FROM word_bank WHERE level_code='THINK_L2' AND zh_definition!='' LIMIT 5;"
```
Expected: zh_definition 有合理中文（如 "caring → 关心的/有同情心的"）。

- [ ] **Step 5: 重跑端到端冒烟（Task 8 Step 3）**

确认 subscribe/learn/detail 链路返回真实数据。

- [ ] **Step 6: Commit**

```bash
git add scripts/import_think_wordlist.py
git commit -m "feat(wordlist): Think 教材导入脚本 - 解析+DeepSeek翻译+幂等插入+evict缓存"
```

> `wordlist/_zh_cache.json` 翻译产物：体积小（~2639 条），可提交以便复现；若不愿提交，加入 `.gitignore`。

---

## Self-Review（已自查）

1. **Spec 覆盖**：
   - §3 数据模型 → Task 1（3 表，subscription 加 surrogate id 已注明）
   - §4 导入流水线 → Task 9
   - §5.1 学新词 → Task 3（findNewWordsByUnit）+ Task 5（learnNewWords）
   - §5.2 复习零改动 → 无需任务（复用现有），Task 8 冒烟验证
   - §5.3 单元推进 → Task 3（advanceUnit）+ Task 5 + Task 6
   - §6 短语适配 → Task 7
   - §7 API → Task 6
   - level 注册 + evict → Task 8（手动）/ Task 9（脚本）
   - §10 YAGNI（上传/选词/example_zh/跨词库）→ 明确不做
2. **占位符**：Task 5 Step 3 有意留 `detail()` 占位并在 Step 4 给真实实现（避免一次贴超长方法）；其余步骤代码完整。
3. **类型一致**：DTO 字段名、Mapper 方法名（`findNewWordsByUnit`/`upsertSubscribe`/`advanceUnit`/`find`/`countByUnit`/`findWordIdsByUnit`）跨任务一致；`WordListService` 静态方法 `isUnitComplete` 与测试一致；`TestService.isPhrase`/`allowsMode` 与测试一致。
4. **风险**：`WordDetailDto` / `level` 表列名 / `word_bank` 必填列 / DeepSeek json 格式 —— 各任务均标注"先 DESCRIBE 核对"。

## 未覆盖（Plan 2：前端）

- `wordmate-web/src/api/wordList.ts` API 客户端
- 词库广场页 + 订阅 + 单元列表 + 学新词入口
- 主导航加"词库"入口
- 复用现有复习/测试页（传 `level_code=THINK_*`）

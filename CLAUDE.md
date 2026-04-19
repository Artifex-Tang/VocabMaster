# CLAUDE.md — 项目主指令

> 本文件是 Claude Code 进入本项目时读取的**主指令**。每次新会话开始先读此文件，再按需读取 `docs/` 下的专题文档。

## 项目代号

**VocabMaster** — 基于艾宾浩斯遗忘曲线的多端背单词应用。

## 核心信息

- **产品形态**：Web（PC 浏览器） + 微信小程序 + Android APP，三端共用一套后端
- **后端主选**：Java 21 + Spring Boot 3.5.x + MyBatis-Plus + MySQL 8.x + Redis 7.x
- **后端备选**：Python 3.12 + FastAPI + SQLAlchemy 2.x + MySQL 8.x + Redis 7.x
- **前端 Web**：Vue 3 + Vite + Pinia + Element Plus
- **前端移动**：Uni-app + Vue 3（一套代码编译出小程序 + Android APK + H5）
- **部署**：Docker Compose（开发）+ Nginx + 后端容器 + MySQL + Redis
- **用户规模目标**：DAU 万级，MySQL 单实例 + Redis 缓存足够

## 关键业务规则（最容易出错，优先记住）

1. **艾宾浩斯九阶段复习**：5 分钟 → 30 分钟 → 12 小时 → 1 天 → 2 天 → 4 天 → 7 天 → 15 天 → 30 天。单词答对 stage+1，答错 stage 回退至 max(1, stage-1)，走完 9 阶视为"已掌握"。详见 `docs/04-ebbinghaus-algorithm.md`。
2. **词库按等级隔离**：十个等级（KET/PET/FCE/CAE + 小学/初中/高中/大学四级/大学六级/专业八级），**同一单词在不同等级是不同记录**（释义深度不同），不做去重合并。
3. **学习进度按「用户 + 单词」维度存储**，不以等级为主键。一个用户可同时学多个等级。
4. **多端进度同步**：所有学习操作都实时同步到服务端，本地只做缓存和离线队列。
5. **离线缓存**：下载一个等级的词库到本地后即可离线学习，复习结果进本地队列，联网后批量上报。
6. **时区**：所有时间字段存 UTC，展示时按用户时区（默认 `Asia/Shanghai`）换算。

## 目录结构（规划）

```
vocabmaster/
├── CLAUDE.md                    # 本文件
├── docs/                        # 规格文档（12 份）
│   ├── 00-README.md             # 项目总览
│   ├── 01-architecture.md       # 架构 + 模块划分
│   ├── 02-database-design.md    # 数据库设计
│   ├── 03-api-specification.md  # API 规范
│   ├── 04-ebbinghaus-algorithm.md
│   ├── 05-backend-java.md       # Java 后端实现
│   ├── 06-backend-python.md     # Python 后端实现（备选）
│   ├── 07-frontend-web.md       # Web 端
│   ├── 08-frontend-uniapp.md    # 小程序 + Android
│   ├── 09-word-data-sourcing.md # 词库采集
│   ├── 10-deployment.md         # 部署
│   └── 11-roadmap.md            # 开发路线图
├── sql/
│   └── init.sql                 # MySQL 初始化脚本
├── seed/
│   └── words_sample.csv         # 种子词库（每级 20 词）
├── prompts/                     # 针对性开发 prompt
│   ├── backend-java.md
│   ├── backend-python.md
│   ├── web.md
│   └── uniapp.md
├── backend-java/                # Java 后端代码（待生成）
├── backend-python/              # Python 后端代码（待生成）
├── frontend-web/                # Web 前端代码（待生成）
├── frontend-uniapp/             # Uni-app 代码（待生成）
└── docker-compose.yml           # 本地开发环境
```

## Claude Code 工作原则

1. **先读文档再动手**：接到任务先找对应的 `docs/` 文件，严格按规格实现，不要自由发挥。
2. **小步提交**：每完成一个模块（比如一个 Controller + Service + Mapper）就停下来让用户确认。
3. **一致性高于优雅**：API 命名、错误码、字段命名三端必须完全一致，参考 `docs/03-api-specification.md`。
4. **遵循标准 Spring Boot 分层规范**：Controller → Service → Mapper/Repository 三层清晰分离，包名按业务模块（`auth/user/word/study/...`）而非技术（`controller/service/...`）组织。详见 `docs/05-backend-java.md`。
5. **遇到不确定的需求**：先查 `docs/`，再查本文件末尾的"FAQ"，都没有的询问用户，不要猜。

## 推荐开发顺序

参考 `docs/11-roadmap.md`。粗略是：

1. 搭数据库 + 种子数据（半天）
2. Java 后端骨架 + 认证模块（1 天）
3. 词库 + 学习进度 API（2 天）
4. Web 端 MVP（2 天）
5. Uni-app 端 MVP（3 天）
6. TTS/听写/拼写测试（2 天）
7. 报表/可视化（2 天）
8. 离线缓存 + 同步（2 天）
9. 部署 + 压测（1 天）

总计约 3 周一人独立完成 MVP。

## FAQ

**Q: 用户注册用手机号还是邮箱？**
A: 都支持。Web 端默认邮箱，小程序端默认微信 OAuth，Android 端默认手机号。见 `docs/03-api-specification.md#认证`。

**Q: TTS 用哪家？**
A: 前端优先用浏览器 / 小程序 / Android 原生 TTS（Web Speech API / uni.createInnerAudioContext + 有道 API / TextToSpeech）。服务端不做 TTS，只返回单词音频 URL（从有道或 Cambridge 词典 CDN）。

**Q: 配图怎么来？**
A: 种子数据用 emoji 兜底。正式词库从 Unsplash API 按词搜图，CDN 缓存 URL 存库。版权敏感场景可换成 Pixabay / Pexels。

**Q: 遗忘曲线可视化用什么图？**
A: 指数衰减曲线 + 用户实际复习点叠加。推荐 ECharts（Web）+ uCharts（Uni-app）。

**Q: 管理后台做吗？**
A: MVP 阶段做一个简版，就用 Web 前端同一个项目加 `/admin` 路由和角色守卫，不单独开项目。

---

下一步：读 `docs/00-README.md` 了解完整产品设计，再按任务需要读专题文档。

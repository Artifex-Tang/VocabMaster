# VocabMaster — 工程规格包

> 基于艾宾浩斯遗忘曲线的多端背单词应用（Web + 微信小程序 + Android）。
> 本仓库**仅包含工程规格文档**，用于驱动 Claude Code 完成从零到 MVP 的开发。

## 快速上手

### 如果你是开发者
1. 先读 [`CLAUDE.md`](./CLAUDE.md) —— 项目主指令与硬约束
2. 再读 [`docs/00-README.md`](./docs/00-README.md) —— 产品形态与整体设计
3. 按照你要做的方向，读对应的 [`prompts/`](./prompts/) 文件

### 如果你是用 Claude Code
在项目根目录开启新会话，第一条输入：
```
请先阅读 CLAUDE.md，然后阅读 prompts/<你要做的端>.md
```

端的对应关系：
- 后端 Java → `prompts/backend-java.md`
- 后端 Python（备选） → `prompts/backend-python.md`
- Web 前端 → `prompts/web.md`
- 移动端（小程序 + Android） → `prompts/uniapp.md`

## 仓库结构

```
vocabmaster/
├── README.md                    # 本文件
├── CLAUDE.md                    # Claude Code 主指令
├── .env.example                 # 环境变量模板
├── .gitignore
├── docker-compose.yml           # 本地开发环境一键启动
│
├── docs/                        # 规格文档（12 份，共约 6400 行）
│   ├── 00-README.md             # 项目总览
│   ├── 01-architecture.md       # 系统架构
│   ├── 02-database-design.md    # 数据库设计（15 张表）
│   ├── 03-api-specification.md  # API 规范（全量接口）
│   ├── 04-ebbinghaus-algorithm.md # 艾宾浩斯九阶段算法
│   ├── 05-backend-java.md       # Java 后端实现
│   ├── 06-backend-python.md     # Python 后端实现（备选）
│   ├── 07-frontend-web.md       # Web 前端实现
│   ├── 08-frontend-uniapp.md    # 小程序 + Android 实现
│   ├── 09-word-data-sourcing.md # 词库采集方案
│   ├── 10-deployment.md         # 部署方案
│   └── 11-roadmap.md            # 3 周 MVP 路线图
│
├── sql/
│   └── init.sql                 # MySQL 初始化脚本（15 张表 + 预置数据）
│
├── seed/
│   └── words_sample.csv         # 种子词库（10 等级 × 20 词 = 200 词）
│
└── prompts/                     # Claude Code 针对性开发 prompt
    ├── backend-java.md
    ├── backend-python.md
    ├── web.md
    └── uniapp.md
```

代码目录在开发时按需创建：
- `backend-java/`
- `backend-python/`
- `frontend-web/`
- `frontend-uniapp/`

## 技术栈一览

| 层 | 技术 | 版本 |
|----|------|------|
| 前端 Web | Vue 3 + Vite + TS + Pinia + Element Plus + ECharts | Vue 3.5+ |
| 移动端 | Uni-app + Vue 3 + uview-plus + uCharts | Uni-app 3.x |
| 后端主 | Java + Spring Boot + MyBatis-Plus | Java 21, SB 3.5.x |
| 后端备 | Python + FastAPI + SQLAlchemy 2.0 async | Python 3.12 |
| 数据库 | MySQL | 8.0+ |
| 缓存 | Redis | 7.x |
| 部署 | Docker Compose + Nginx | — |

## 三端共用的硬约束

1. **艾宾浩斯九阶段间隔**（小时）：`[5/60, 0.5, 12, 24, 48, 96, 168, 360, 720]`
   - 5 分钟 → 30 分钟 → 12 小时 → 1 天 → 2 天 → 4 天 → 7 天 → 15 天 → 30 天
2. **时区**：DB 存 UTC，前端按用户时区（默认 Asia/Shanghai）展示
3. **统一响应格式**：`{ code, msg, data, request_id }`
4. **JWT**：HS256，access 2h + refresh 7d，Java 和 Python 共用 secret
5. **单词等级隔离**：同一单词在不同等级是不同记录（不去重）

## 启动本地开发环境

```bash
# 1. 克隆后（假设这个仓库就是 vocabmaster/）
cd vocabmaster
cp .env.example .env
# 编辑 .env，至少把 VM_JWT_SECRET 和 VM_PHONE_AES_KEY 换成随机值

# 2. 起中间件
docker compose up -d mysql redis

# 3. 导入 schema + 种子数据
docker exec -i vocab-mysql mysql -uroot -proot vocabmaster < sql/init.sql
# 种子词库在后端启动时自动导入（backend 会检测 word_bank 是否为空）

# 4. 启动后端（任选其一）
cd backend-java && ./mvnw spring-boot:run     # Java，端口 8080
# 或
cd backend-python && uvicorn app.main:app --reload --port 8081

# 5. 启动前端
cd frontend-web && pnpm install && pnpm dev    # 端口 5173

# 6. 移动端
cd frontend-uniapp && pnpm install && pnpm dev:h5         # H5 调试
# 或用 HBuilderX 打开，编译到小程序 / Android
```

## 开发路线图

### Week 1：数据层 + 后端骨架
- ✅ SQL schema 就绪
- ✅ 种子词库就绪
- 📋 Java 后端：骨架 + 认证 + 词库 + 学习接口
- 📋 单元测试：艾宾浩斯算法 100% 覆盖

### Week 2：三端 MVP
- 📋 Web 端：登录 + 今日学习 + 会话 + 结果
- 📋 Uni-app 端：H5 跑通 → 小程序 → Android APK
- 📋 离线缓存 + 批量同步

### Week 3：增强 + 部署
- 📋 TTS + 测试模式（拼写/选择/听写）
- 📋 学习报表 + 遗忘曲线可视化
- 📋 打卡 + 连续天数 + 成就
- 📋 部署到生产 + 监控

详见 [`docs/11-roadmap.md`](./docs/11-roadmap.md)。

## 给 Claude Code 的使用建议

本规格包是为**多轮对话、多端并行开发**设计的：

1. **一个端一个项目会话**：不要让一个会话同时做后端 + 前端，token 消耗太快
2. **每次开会话先读 CLAUDE.md**：确保 Claude 有完整背景
3. **按 `prompts/` 顺序执行**：每份 prompt 都有"第一条回复"约定，帮助确认 Claude 理解正确
4. **遇到决策点停下来问**：所有工程决策都在 `docs/` 里定了，如果 Claude 想"自由发挥"，提醒它回去读文档

## 文档版本

- 规格版本：v1.0
- 最后更新：2026-04-18
- 贡献：@城外人 + Claude (Anthropic)

## License

工程规格文档：CC BY 4.0
生成的代码：由开发者自行选择 license

# Wordmate Mini 微信小程序调试与完善设计

> 日期：2026-06-02
> 状态：待实施

## 背景

wordmate-mini（uni-app）项目已有完整的代码骨架，包括 12 个页面、API 层、Store、工具函数和组件。但从未实际运行测试过。需要全量调试、修复问题、对齐 Web 端功能，最终在 H5 和微信开发者工具中完成测试。

## 现有资产

### 已完成的模块
- **12 个页面**：login, register, index(Dashboard), study/session, study/done, test/index, test/choice, test/spelling, test/listening, stats/index, mine/index, wrong-book/list
- **API 层**：auth, word, study, stats, sync, types（完整）
- **Store**：user, study, settings（Pinia）
- **工具**：request(HTTP+token), tts(跨平台), storage, platform, device-id, date
- **组件**：WordCard, EbbinghausChart
- **离线同步**：useOfflineSync composable

### 技术栈
- uni-app 3.x + Vue 3 + TypeScript + Vite + Pinia
- uview-plus UI 组件库
- 目标平台：H5 / 微信小程序 / Android

## 实施策略

**方案 A+C 结合**：按页面顺序调试，H5 模式为主，每完成一批检查小程序兼容性。

## 分阶段计划

### Phase 1：环境搭建与基础验证（~2h）

1. **安装依赖**：`pnpm install`
2. **H5 启动验证**：`pnpm dev:h5`，确认能跑起来
3. **检查 uview-plus 集成**：确保组件注册、样式引入正确
4. **修复编译错误**：解决首次运行的所有 TS/Vue 编译报错
5. **API 连通性**：配置后端地址，确认 HTTP 请求能到达

### Phase 2：认证模块（~1h）

6. **登录页面**：对齐 Web 端逻辑（邮箱/手机号识别、密码显示切换）
7. **注册页面**：邮箱注册流程，验证码（手机注册暂时 skip）
8. **Token 持久化**：验证 token 存储、刷新、自动登录
9. **路由守卫**：未登录跳转登录页

### Phase 3：核心学习流程（~3h）

10. **Dashboard**：等级选择、今日计划加载、每日签到
11. **学习卡片**：翻转动画、认识/不认识/跳过、音频播放、进度追踪
12. **学习完成**：统计展示、返回导航
13. **离线同步**：IndexedDB 写入、联网后批量上报

### Phase 4：测试模块（~2h）

14. **测试入口**：模式选择、等级/数据源/数量配置
15. **选择题**：四选一、自动翻页、跳过、提交
16. **拼写测试**：中/英文释义切换、音频播放、输入验证
17. **听写测试**：音频播放、拼写输入

### Phase 5：统计与辅助（~1.5h）

18. **统计概览**：今日数据、30天柱状图
19. **遗忘曲线**：单词搜索、曲线图表
20. **错词本**：列表、等级筛选、移除、开始复习
21. **个人中心**：用户信息、连续学习天数、退出登录
22. **设置页面**：学习偏好、主题切换

### Phase 6：微信小程序适配（~2h）

23. **微信开发者工具启动**：`pnpm dev:mp-weixin`
24. **wx.login 集成**：微信一键登录流程
25. **平台兼容修复**：DOM API、CSS 差异、API 限制
26. **小程序特有配置**：权限声明、域名白名单

### Phase 7：全量测试（~1h）

27. **H5 端全流程回归**
28. **微信开发者工具全流程回归**
29. **边界情况测试**：网络断开、token 过期、空数据

## 验收标准

- [ ] H5 模式全流程可运行（注册→登录→选等级→学习→测试→统计→错词本）
- [ ] 微信开发者工具全流程可运行
- [ ] 所有 API 调用正确（参数命名、响应解析）
- [ ] 音频播放正常（TTS + 有道 CDN 兜底）
- [ ] 离线模式下学习可进行，联网后同步
- [ ] 无 TypeScript 编译错误
- [ ] 无控制台报错

## 风险与注意事项

1. **uview-plus 与 uni-app 3.x 兼容性**：可能需要版本调整
2. **小程序 API 限制**：`uni.createInnerAudioContext` vs `wx.createInnerAudioContext`
3. **CSS 差异**：小程序不支持部分 CSS 特性（flex gap、部分选择器）
4. **包大小限制**：微信小程序主包 2MB，可能需要分包
5. **异步 API**：`uni.xxx` 多数是回调式，需 Promise 化

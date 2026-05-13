# TeamFiles 局域网团队文件管理系统 — 开发计划

## Context

项目将从 `filmtab` 重命名为 `teamfiles`（包名 `com.myself.teamfiles`），基于 Spring Boot 3.5.14 + Java 21 骨架，从零构建完整的 **前后端** 系统。需求涵盖 7 大模块：用户管理、文件管理、标签管理、搜索、回收站、统计概览、AI 对接。目标用户是局域网内 20 人以下的小型团队。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Axios + Pinia + Vue Router |
| 后端 | Spring Boot 3.5.14 + Spring Security + MyBatis-Plus + Java 21 |
| 数据库 | MySQL 8.4 + Redis |
| 全文检索 | Elasticsearch + IK 分词器 |
| AI | LangChain4j（支持 OpenAI/Claude/国内大模型/本地部署） |
| 工具 | Hutool、Lombok、Knife4j |
| 构建 | Maven (后端) + Vite (前端) |
| 部署 | Docker Compose / Windows 直接运行 |

---

## 一、重命名操作（实施前第一步）

1. `pom.xml`: groupId → `com.myself`, artifactId → `teamfiles`, name → `teamfiles`
2. Java 包名: `com.myself.filmtab` → `com.myself.teamfiles`
3. 目录重构: `src/main/java/com/myself/filmtab/` → `src/main/java/com/myself/teamfiles/`
4. `FilmtabApplication.java` → `TeamfilesApplication.java`
5. `application.properties`: `spring.application.name=teamfiles`

---

## 二、后端包结构

```
com.myself.teamfiles
├── TeamfilesApplication.java
├── common/
│   ├── config/       # SecurityConfig, MybatisPlusConfig, RedisConfig, FileUploadConfig,
│   │                   # ElasticsearchConfig, AsyncConfig, WebMvcConfig
│   ├── constant/     # FileConstants, UserConstants, SystemConstants
│   ├── enums/        # UserRoleEnum(ADMIN/FORMAL/GUEST), FileTypeEnum, StorageSpaceEnum,
│   │                   # SearchLogicEnum(AND/OR), ApiTypeEnum
│   ├── exception/    # GlobalExceptionHandler, BusinessException, ErrorCode
│   ├── result/       # R.java(统一响应{code,msg,data}), PageResult
│   ├── util/         # JwtTokenUtil, FileUtil, ZipUtil, EncryptUtil(AES)
│   ├── annotation/   # @OperationLog, @RequireAdmin
│   └── aspect/       # OperationLogAspect, RateLimiterAspect
├── security/
│   ├── filter/       # JwtAuthenticationFilter, LoginAttemptFilter
│   ├── handler/      # CustomAccessDeniedHandler, CustomAuthenticationEntryPoint
│   └── service/      # UserDetailsServiceImpl
├── module/
│   ├── user/         # controller/service/mapper/entity/dto
│   ├── file/         # controller/service/mapper/entity/dto (含 ChunkUploadService 分片上传)
│   ├── tag/          # controller/service/mapper/entity/dto (含 TagGroup)
│   ├── search/       # controller/service/mapper/entity/dto/repository(ES)
│   ├── recycle/      # controller/service/mapper/entity/dto
│   ├── statistics/   # controller/service/dto
│   ├── ai/           # controller/service/mapper/entity/dto (LangChain4j)
│   └── log/          # controller/service/mapper/entity/dto (操作日志)
└── scheduled/        # RecycleCleanTask, BackupTask, LoginUnlockTask, ChunkCleanTask
```

---

## 三、数据库核心表（14 张）

| # | 表名 | 核心字段 | 说明 |
|---|---|---|---|
| 1 | sys_user | username/password(BCrypt)/role(ADMIN,FORMAL,GUEST)/status/first_login/login_fail_count/locked_until/storage_quota/last_login_time | 用户表 |
| 2 | file_info | file_name/original_name/file_path/file_size/file_type/mime_type/file_extension/md5/storage_space(PERSONAL,TEAM)/folder_id/upload_user_id | 文件信息 |
| 3 | folder | folder_name/parent_id(0=根)/storage_space/owner_user_id/sort_order | 文件夹(自关联) |
| 4 | file_chunk | upload_id/chunk_index/chunk_count/chunk_size/chunk_path/status/expired_at | 分片上传记录 |
| 5 | tag | tag_name/color/creator_user_id/deleted(弱删除) | 标签 |
| 6 | user_tag_visible | user_id/tag_id | 用户标签可见范围 |
| 7 | file_tag | file_id/tag_id/created_by | 文件-标签关联 |
| 8 | tag_group | group_name/owner_user_id/color/sort_order | 标签组(用户私有) |
| 9 | tag_group_item | group_id/tag_id | 标签组-标签关联 |
| 10 | recycle_bin | original_type(FILE,FOLDER)/original_id/file_name/file_path/file_size/storage_space/deleted_by/expire_at | 回收站 |
| 11 | search_history | user_id/keyword/search_type/result_count | 搜索历史 |
| 12 | search_template | user_id/template_name/search_condition(JSON) | 搜索模板 |
| 13 | ai_api_config | user_id/is_system/api_type/api_key(AES加密)/api_base_url/model_name/is_active/tested | AI配置 |
| 14 | operation_log | user_id/username/module/operation/method/request_params/ip/execution_time/result | 操作日志 |

---

## 四、API 接口概览（基础路径 /api/v1）

### 4.1 认证
```
POST /api/v1/auth/login          # 登录(rememberMe→7天token)
POST /api/v1/auth/logout         # 登出(token黑名单)
POST /api/v1/auth/refresh        # 刷新token
GET  /api/v1/auth/me             # 当前用户信息
PUT  /api/v1/auth/password       # 首次登录强制改密码
```

### 4.2 管理员-用户管理
```
POST   /api/v1/admin/users                    # 创建用户
GET    /api/v1/admin/users                    # 用户列表(分页+筛选)
GET    /api/v1/admin/users/{id}               # 用户详情
PUT    /api/v1/admin/users/{id}               # 修改用户(角色/状态)
DELETE /api/v1/admin/users/{id}               # 删除(?fileHandle=TRANSFER/DELETE)
PUT    /api/v1/admin/users/{id}/reset-password # 重置密码
PUT    /api/v1/admin/users/{id}/status        # 启用/禁用
```

### 4.3 个人信息
```
PUT  /api/v1/profile/nickname    # 改昵称
PUT  /api/v1/profile/avatar      # 上传头像
PUT  /api/v1/profile/password    # 修改密码(旧密码+新密码)
GET  /api/v1/profile/storage     # 存储空间使用情况
```

### 4.4 文件管理
```
POST   /api/v1/files/upload              # 单文件上传
POST   /api/v1/files/batch-upload        # 批量上传(≤20个)
POST   /api/v1/files/chunk/init          # 初始化分片上传
POST   /api/v1/files/chunk/upload        # 上传分片
POST   /api/v1/files/chunk/merge         # 合并分片
POST   /api/v1/files/chunk/cancel        # 取消上传
GET    /api/v1/files/chunk/progress      # 上传进度(?uploadId=)
GET    /api/v1/files                     # 文件列表(分页+筛选)
GET    /api/v1/files/{id}                # 文件详情
PUT    /api/v1/files/{id}/rename         # 重命名
DELETE /api/v1/files/{id}                # 删除(→回收站)
POST   /api/v1/files/{id}/copy           # 复制
POST   /api/v1/files/{id}/move           # 移动
POST   /api/v1/files/{id}/move-to-team   # 个人→团队空间
GET    /api/v1/files/{id}/preview        # 预览
GET    /api/v1/files/{id}/download       # 下载
POST   /api/v1/files/batch-download      # 批量ZIP下载
GET    /api/v1/files/{id}/similar        # 相似文件
```

### 4.5 文件夹
```
POST   /api/v1/folders             # 创建
GET    /api/v1/folders             # 列表
GET    /api/v1/folders/{id}        # 详情
PUT    /api/v1/folders/{id}/rename # 重命名
DELETE /api/v1/folders/{id}        # 删除(→回收站)
GET    /api/v1/folders/tree        # 文件夹树
POST   /api/v1/folders/batch-delete # 批量删除
```

### 4.6 标签
```
POST   /api/v1/tags                  # 创建标签
GET    /api/v1/tags                  # 用户可见标签列表
PUT    /api/v1/tags/{id}             # 编辑
DELETE /api/v1/tags/{id}             # 弱删除(仅移除可见性)
GET    /api/v1/tags/{id}/files       # 关联文件
GET    /api/v1/tags/similar          # 相似推荐(?keyword=)
GET    /api/v1/tags/autocomplete     # 自动补全(?keyword=)
POST   /api/v1/files/{fileId}/tags   # 文件打标签
DELETE /api/v1/files/{fileId}/tags/{tagId} # 移除标签
POST   /api/v1/files/batch-tags      # 批量打/移除标签
```

### 4.7 标签组
```
POST   /api/v1/tag-groups                   # 创建标签组
GET    /api/v1/tag-groups                   # 用户标签组列表
PUT    /api/v1/tag-groups/{id}              # 编辑
DELETE /api/v1/tag-groups/{id}              # 删除组(不删标签)
POST   /api/v1/tag-groups/{id}/tags         # 添加标签入组
DELETE /api/v1/tag-groups/{id}/tags/{tagId} # 从组移除标签
```

### 4.8 搜索
```
POST   /api/v1/search              # 综合搜索(ES)
GET    /api/v1/search/suggest      # 搜索建议
GET    /api/v1/search/history      # 搜索历史
DELETE /api/v1/search/history      # 清空历史
DELETE /api/v1/search/history/{id} # 删除单条历史
POST   /api/v1/search/templates    # 保存模板
GET    /api/v1/search/templates    # 模板列表
PUT    /api/v1/search/templates/{id}  # 编辑模板
DELETE /api/v1/search/templates/{id}  # 删除模板
POST   /api/v1/search/templates/{id}/execute # 执行模板
```

### 4.9 回收站
```
GET    /api/v1/recycle           # 列表
POST   /api/v1/recycle/{id}/restore # 恢复
DELETE /api/v1/recycle/{id}      # 永久删除
DELETE /api/v1/recycle/batch     # 批量永久删除
DELETE /api/v1/recycle/clear     # 清空(?storageSpace=)
```

### 4.10 统计
```
GET /api/v1/statistics/dashboard     # 首页概览
GET /api/v1/statistics/storage       # 存储统计
GET /api/v1/statistics/file-types    # 文件类型分布
GET /api/v1/statistics/recent-files  # 最近上传
GET /api/v1/statistics/hot-tags      # 常用标签
GET /api/v1/admin/statistics/overview # 管理员全局统计
```

### 4.11 AI 模块
```
POST   /api/v1/admin/ai/config        # 管理员全局AI配置
PUT    /api/v1/admin/ai/config/{id}   # 修改全局AI
POST   /api/v1/ai/config              # 用户自配AI
PUT    /api/v1/ai/config/{id}         # 修改AI配置
DELETE /api/v1/ai/config/{id}         # 删除AI配置
GET    /api/v1/ai/config              # 当前可用配置
POST   /api/v1/ai/config/{id}/test    # 测试连接
POST   /api/v1/ai/chat/summary        # 文件摘要
POST   /api/v1/ai/chat/qa             # 智能问答(SSE流式)
GET    /api/v1/ai/chat/conversations  # 对话列表
GET    /api/v1/ai/chat/conversations/{id} # 对话详情
DELETE /api/v1/ai/chat/conversations/{id} # 删除对话
```

### 4.12 系统管理
```
GET    /api/v1/admin/logs              # 操作日志
DELETE /api/v1/admin/logs/clean        # 清理日志
POST   /api/v1/admin/backup/create     # 创建备份
GET    /api/v1/admin/backup/list       # 备份列表
POST   /api/v1/admin/backup/{id}/restore # 恢复备份
```

---

## 五、前端项目结构

```
teamfiles-web/                     # 前端项目根目录
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── .env.development               # 开发环境API地址
├── .env.production                # 生产环境API地址
└── src/
    ├── main.ts                    # 入口，注册Element Plus、Router、Pinia
    ├── App.vue                    # 顶层布局(根据登录状态切换)
    ├── router/
    │   └── index.ts               # 路由配置(含权限守卫)
    ├── stores/
    │   ├── user.ts                # 用户状态(token/userInfo/permissions)
    │   ├── file.ts                # 文件浏览状态(当前路径/视图模式)
    │   ├── tag.ts                 # 标签缓存
    │   └── app.ts                 # 全局UI状态(侧边栏/主题)
    ├── api/
    │   ├── request.ts             # Axios实例(拦截器:token注入/401处理)
    │   ├── auth.ts                # 认证API
    │   ├── user.ts                # 用户管理API
    │   ├── file.ts                # 文件API
    │   ├── folder.ts              # 文件夹API
    │   ├── tag.ts                 # 标签API
    │   ├── search.ts              # 搜索API
    │   ├── recycle.ts             # 回收站API
    │   ├── statistics.ts          # 统计API
    │   └── ai.ts                  # AI API
    ├── views/
    │   ├── login/                 # 登录页
    │   ├── dashboard/             # 首页概览(存储统计/最近文件/常用标签)
    │   ├── files/                 # 文件管理(含文件夹树+文件列表)
    │   ├── preview/               # 文件预览(图片/PDF/Office)
    │   ├── tags/                  # 标签管理(含标签组)
    │   ├── search/                # 搜索结果页
    │   ├── recycle/               # 回收站
    │   ├── admin/                 # 管理员-用户管理
    │   ├── profile/               # 个人信息
    │   ├── ai/                    # AI对话
    │   └── system/                # 系统设置(AI配置/备份/日志)
    ├── components/
    │   ├── layout/                # AppLayout(Header+Sidebar+Content)
    │   ├── file/                  # FileUpload(拖拽上传), FileCard, FileTable, FilePreview
    │   ├── tag/                   # TagSelector, TagInput(自动补全), TagBadge
    │   ├── folder/                # FolderTree, FolderSelector
    │   ├── search/                # SearchBar, SearchFilter, SearchSuggest
    │   ├── common/                # Pagination, EmptyState, Loading, ConfirmDialog
    │   └── ai/                    # ChatWindow, ChatMessage, ToolConfirmDialog
    ├── composables/               # useUpload, useSearch, useWebSocket, usePermission
    ├── utils/                     # format, validate, download, preview
    └── types/                     # TypeScript类型定义
```

### 前端路由设计

| 路径 | 视图 | 权限 | 说明 |
|---|---|---|---|
| /login | LoginPage | 公开 | 登录页 |
| / | DashboardPage | 已登录 | 首页概览 |
| /files | FileListPage | 已登录 | 文件管理(默认个人空间) |
| /files/team | FileListPage | 正式用户+ | 团队空间文件 |
| /files/:id/preview | FilePreviewPage | 已登录 | 文件预览 |
| /tags | TagManagePage | 已登录 | 标签管理 |
| /search | SearchResultPage | 已登录 | 搜索结果 |
| /recycle | RecycleBinPage | 已登录 | 回收站 |
| /recycle/team | RecycleBinPage | 管理员 | 团队回收站 |
| /admin/users | AdminUsersPage | 管理员 | 用户管理 |
| /admin/system | SystemConfigPage | 管理员 | 系统设置 |
| /profile | ProfilePage | 已登录 | 个人信息 |
| /ai/chat | AiChatPage | 已登录+已配置AI | AI对话 |

### 前端状态管理 (Pinia Stores)

**userStore**: token, userInfo(userId/username/role/nickname/avatar), permissions, login(), logout(), fetchUserInfo()
**fileStore**: currentPath, viewMode(list/grid), selectedFiles, selectedFolder, fileList, breadcrumb
**tagStore**: userTags, tagGroups, fetchTags(), addTag(), removeTag()
**appStore**: sidebarCollapsed, theme(light), uploadQueue

---

## 六、开发阶段（共 7 个阶段，约 9 周）

### 第一阶段：项目重命名 + 基础框架 + 用户管理（约 1 周）

**后端：**
- 重命名项目: filmtab → teamfiles (pom.xml/包名/类名/配置)
- 完善 pom.xml 全部依赖
- application.yml 配置（数据源/Redis/JWT/文件存储路径）
- 通用模块: R.java, GlobalExceptionHandler, BusinessException, ErrorCode, JwtTokenUtil
- Spring Security + JWT（登录/登出/刷新/记住我7天/登录失败5次锁定10分钟）
- 数据库初始化 SQL（14 张表 + admin 初始数据 + BCrypt 密码）
- 用户管理 CRUD（管理员创建/查看/修改角色状态/删除/重置密码/启用禁用）
- 个人信息管理（改密码/昵称/上传头像）
- @OperationLog 注解 + AOP + 操作日志表
- 跨域配置 + Knife4j API 文档

**前端：**
- Vite + Vue 3 + TypeScript + Element Plus 项目初始化
- 基础布局组件（AppLayout: Header + Sidebar + Content）
- Axios 封装（token 注入 + 401 拦截 + 统一错误处理）
- 登录页 + 首次登录强制改密码页
- 路由权限守卫（未登录跳登录页，角色控制页面访问）
- Pinia userStore

### 第二阶段：文件管理核心（约 2 周）

**后端：**
- 单文件上传 + 批量上传（最多 20 个）
- 分片上传：初始化/上传分片/合并/取消/进度查询
- MD5 秒传（上传前检查 MD5 去重）
- 24h 未完成上传的清理定时任务
- 文件操作：重命名/删除(→回收站)/复制/移动/个人→团队空间
- 文件夹管理：创建/重命名/删除/层级树/批量操作
- 文件预览：图片流式返回 + PDF 流式返回 + Office 通过 KKFileView
- 单文件下载 + 批量 ZIP 打包下载(异步压缩)
- 权限控制：个人空间(上传者+管理员) vs 团队空间(正式用户可读/管理员全权限/访客仅预览下载)

**前端：**
- 文件管理页面（左侧文件夹树 + 右侧文件列表）
- 列表/网格视图切换
- 文件夹树组件（支持展开/折叠/右键菜单）
- 文件上传组件（拖拽上传 + 点击上传 + 标签选择 + 进度条）
- 分片上传进度展示 + 取消上传
- 文件预览页（图片: 放大缩小旋转幻灯片; PDF: pdf.js 渲染; Office: KKFileView iframe）
- 文件操作菜单（重命名/删除/复制/移动/个人转团队）
- 批量下载（多选后打包下载，显示进度）
- Pinia fileStore

### 第三阶段：标签管理 + 回收站（约 1 周）

**后端：**
- 标签 CRUD（弱删除：移除 user_tag_visible，保留 tag 和 file_tag）
- 文件-标签关联：打标签/移除标签/批量操作
- 标签可见范围：创建标签/上传时选择/关联文件时自动添加可见性
- 相似标签推荐（Levenshtein 编辑距离 <= 2）
- 标签自动补全（前缀匹配）
- 标签组：创建/编辑/删除/添加移除标签
- 回收站：列表(个人/团队分开)/恢复/永久删除/批量删除/清空
- 30 天自动清理定时任务

**前端：**
- 标签管理页面（标签网格 + 标签组侧栏）
- TagInput 组件（输入时自动补全 + 相似标签提醒）
- TagBadge 组件（颜色标签 + 文件数量 badge）
- 标签选择器（上传时/文件详情中批量添加标签）
- 标签组管理（拖拽标签入组）
- 回收站页面（文件列表 + 恢复/永久删除/清空按钮 + 过期倒计时）

### 第四阶段：搜索模块（约 1.5 周）

**后端：**
- ES 集成配置 + 索引设计 + IK 分词器安装
- 文件索引同步（上传时异步索引、更新时更新索引、删除时移除索引）
- 基础搜索：文件名 + 文件内容全文检索 + 文件类型 + 时间范围 + 大小范围 + 搜索范围(个人/团队/全部)
- 标签搜索：单标签 + 多标签 AND/OR 逻辑
- 搜索结果：ES highlight 高亮 + 排序(相关度/时间/大小/名称)
- 搜索建议（基于前缀 + 历史）
- 搜索历史记录（保存 + 快速重复搜索 + 清除）
- 相似文件推荐（ES more_like_this + 标签 Jaccard 相似度）
- 搜索模板：保存/加载/编辑/删除/执行
- 全量索引重建接口

**前端：**
- 全局搜索栏组件（Header 中，支持关键词 + 范围选择）
- 搜索结果页（列表/网格切换 + 关键词高亮 + 排序选项）
- 搜索筛选面板（标签/文件类型/时间范围/大小范围）
- 搜索建议下拉（实时显示相关文件名/标签/类型）
- 搜索历史展示
- "找相似"功能入口 + 相似文件推荐展示
- 搜索模板管理（保存当前筛选条件/加载已保存模板）
- 文件内容搜索关键词高亮

### 第五阶段：统计概览 + 系统管理（约 0.5 周）

**后端：**
- 首页概览 API（存储空间使用/文件类型分布饼图数据/最近上传/常用标签Top20）
- 管理员全局统计（团队总存储/总文件数/总用户数/活跃度）
- 数据备份：手动创建（导出SQL+文件归档）+ 备份列表 + 恢复

**前端：**
- 首页 Dashboard（ECharts 饼图 + 统计卡片 + 最近文件列表 + 常用标签云）
- 管理员统计页（团队概览看板）
- 系统设置页（备份管理/日志查看/全局AI配置）

### 第六阶段：AI 模块（约 1.5 周）

**后端：**
- AI API 配置管理（管理员全局配置 + 用户自配，AES-256 加密存储）
- 测试 API 连接
- LangChain4j 集成（多模型适配：OpenAI/Claude/通义千问/智谱/本地部署）
- 文件文本提取（Apache POI for Word/Excel, PDFBox for PDF, 纯文本读取）
- 文件摘要：提取文件内容 → 发送给 AI → 返回摘要
- 智能问答：多轮对话 + RAG（引用文件内容作为上下文）
- AI Tool：数据库查询工具（`@Tool` 注解方法，如查询标签总数/搜索文件）
- Tool 调用安全：SSE 通知前端，用户确认后才执行
- 对话历史保存/查看/删除

**前端：**
- AI 配置页面（表单：API类型/Key/Base URL/模型名称 + 测试连接按钮）
- AI 对话页面（左侧对话列表 + 右侧聊天窗口）
- 聊天窗口（消息气泡 + Markdown 渲染 + 文件引用入口）
- 引用文件选择弹窗（选择要询问的文件）
- Tool 确认弹窗（"AI需要查询数据库中的标签总数，是否允许？"）
- SSE 流式接收回复
- 对话历史切换/删除

### 第七阶段：部署 + 优化 + 文档（约 1.5 周）

- Docker Compose 编排（MySQL 8.4 + Redis 7 + Elasticsearch 8 + KKFileView + 应用 + Nginx）
- Windows 直接部署文档（JDK 21 + 启动脚本 + 外部依赖安装说明）
- 前端 build + 集成到 Spring Boot static 目录或 Nginx 代理
- JMeter 压力测试（20 并发 → 搜索 <1s, 预览 <2s）
- 系统使用帮助文档
- 一键启动/停止脚本（Windows .bat + Linux .sh）

---

## 七、关键技术方案

| 技术点 | 方案 |
|---|---|
| JWT 认证 | jjwt 库，普通 2h / 记住我 7d，Redis 黑名单管理登出，BCrypt 加密密码 |
| 登录锁定 | Redis TTL 10 分钟记录失败次数，达 5 次锁定 locked_until 字段，定时任务解锁 |
| 分片上传 | 5MB/片，uploadId(UUID) 追踪，临时目录存储，合并后校验 MD5，24h 未完成自动清理 |
| 秒传 | 上传前查 MD5，已存在则直接引用（增加引用计数） |
| 文件存储 | `{basePath}/{PERSONAL\|TEAM}/{YYYY}/{MM}/{DD}/{uuid}.{ext}`，物理 UUID 命名防冲突 |
| 图片预览 | 直接返回文件流，前端 CSS 实现放大缩小旋转 |
| PDF 预览 | 后端 Range 分片流，前端 pdf.js 渲染 |
| Office 预览 | KKFileView 独立容器，后端转发预览 URL |
| ZIP 下载 | @Async 异步压缩，流式 ZipOutputStream 写入，完成后通知前端，定时清理临时 ZIP |
| 标签弱删除 | tag.deleted 标记软删，user_tag_visible 控制可见范围，file_tag 关联不受影响 |
| 相似标签 | Levenshtein 编辑距离算法，编辑距离 ≤2 推荐，提醒用户避免重复 |
| ES 搜索 | BoolQuery 组合，multi_match 搜索文件名+内容，IK 分词器，异步同步索引 |
| 相似文件 | ES more_like_this + 标签 Jaccard 系数，综合排序 |
| AI Key 安全 | AES-256 加密存储，解密后使用，配置信息用户隔离 |
| AI Tool 安全 | @Tool 方法被 AI 调用时，SSE 通知前端让用户确认，确认后才执行 |
| 操作日志 | @OperationLog 注解 + AOP + @Async 异步写入，不影响主流程性能 |
| 回收站清理 | @Scheduled 每天凌晨 2 点扫描 expire_at < now() 的记录，物理删除文件和数据库记录 |
| Redis 缓存 | 用户信息 30min / 搜索建议 1h / 标签推荐 30min / 首页统计 5min |

---

## 八、验证方式

1. **后端单测**: 每个 Service 编写单元测试，覆盖率 > 60%
2. **接口测试**: Knife4j 文档页直接调试每个 API
3. **集成测试**: @SpringBootTest 验证关键业务流程（登录→上传→搜索→删除→恢复）
4. **前端联调**: 每个阶段完成后前后端联调，确认数据流转和 UI 交互正确
5. **压力测试**: JMeter 模拟 20 并发，确认搜索 <1s、预览 <2s、上传无异常
6. **兼容性测试**: Chrome/Firefox/Edge 三个浏览器验证

---

## 九、文件清单（本次实施涉及的关键文件）

### 后端关键文件
- `pom.xml` — Maven 依赖管理
- `src/main/resources/application.yml` — 全部配置
- `src/main/resources/db/schema.sql` — 数据库初始化 DDL
- `src/main/resources/db/data.sql` — 初始数据（admin 用户）
- `src/main/java/com/myself/teamfiles/TeamfilesApplication.java` — 启动类

### 前端关键文件
- `teamfiles-web/package.json` — 依赖
- `teamfiles-web/vite.config.ts` — 构建配置
- `teamfiles-web/src/App.vue` — 根组件
- `teamfiles-web/src/router/index.ts` — 路由
- `teamfiles-web/src/stores/user.ts` — 用户状态
- `teamfiles-web/src/api/request.ts` — Axios 封装

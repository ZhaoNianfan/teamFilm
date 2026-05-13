# TeamFiles - 局域网团队文件管理系统

基于标签的局域网团队文件管理系统，通过"标签+搜索"替代传统文件夹管理，实现文件快速上传、智能分类和精准检索。

## 技术栈

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Axios + Pinia + Vue Router + ECharts |
| 后端 | Spring Boot 3.5.14 + Spring Security + JWT + MyBatis-Plus + Java 21 |
| 数据库 | MySQL 8.4 + Redis |
| 全文检索 | Elasticsearch 8.19（可选） |
| AI | LangChain4j（支持 OpenAI / 通义千问 / 智谱GLM / Claude / 本地部署） |
| 构建 | Maven（后端）+ Vite（前端） |

## 运行环境

- **JDK** 21+
- **MySQL** 8.4（端口 3306，默认 root/123456）
- **Redis** 7（端口 6379，无密码）
- **Elasticsearch** 8.x（可选，端口 9200，未安装时搜索自动回退 MySQL LIKE）
- **Node.js** 18+（前端开发）

## 快速启动

### 方式一：免安装部署（推荐）

**只需安装 Java 21**，无需 MySQL、Redis、Node.js。

```bash
# 1. 克隆项目
git clone https://github.com/ZhaoNianfan/teamFilm.git
cd teamFilm

# 2. 双击运行
deploy.bat    # 首次运行自动构建前端+后端，之后直接启动
```

浏览器打开 `http://localhost:8088`，默认账号 **admin** / **admin123**。

H2 数据库文件存储在 `data/db/` 目录，数据持久化，重启不丢失。

### 方式二：开发模式

需要 MySQL 8.4 + Redis 7 + Node.js 18+。

```bash
# 后端 (端口 8088)
mvnw spring-boot:run

# 前端 (端口 5173)
cd teamfiles-web
npm install
npm run dev
```

浏览器打开 `http://localhost:5173`。
- 首次登录强制修改密码

### API 文档

`http://localhost:8088/doc.html`（Knife4j）

## 项目结构

```
filmTab/
├── src/main/java/com/myself/teamfiles/    # 后端源码
│   ├── common/               # 配置、常量、异常、工具、AOP
│   ├── security/             # Spring Security + JWT 鉴权
│   ├── module/
│   │   ├── user/             # 用户管理
│   │   ├── file/             # 文件管理（上传/下载/预览/文件夹）
│   │   ├── tag/              # 标签管理（标签/标签组/文件关联）
│   │   ├── search/           # 搜索（ES + MySQL 回退）
│   │   ├── recycle/          # 回收站
│   │   ├── statistics/       # 统计概览
│   │   ├── ai/               # AI 对话（OpenAI 兼容接口）
│   │   └── log/              # 操作日志
│   └── scheduled/            # 定时任务（回收站清理、分片清理）
├── src/main/resources/
│   ├── application.yml       # 主配置
│   └── db/schema.sql         # 数据库 DDL（14 张表）
├── teamfiles-web/            # 前端源码
│   └── src/
│       ├── api/              # API 封装
│       ├── views/            # 页面
│       ├── components/       # 组件
│       ├── stores/           # Pinia 状态
│       └── router/           # 路由
└── pom.xml
```

## 功能模块

### 用户管理
- 管理员创建/编辑/删除账号（正式用户/访客）
- JWT 认证（默认 30min，记住我 7 天）
- 登录失败 5 次锁定 10 分钟
- BCrypt 密码加密

### 文件管理
- 单文件/批量上传（最大 5GB，最多 20 个同时）
- 分片上传 + MD5 秒传
- 拖拽上传 / 点击上传
- 文件夹管理（创建/重命名/删除/层级树）
- 文件操作：预览、下载、重命名、复制、移动、移至团队空间
- 图片预览（缩放/旋转）、PDF 预览、文本预览
- 批量 ZIP 打包下载
- 个人空间 / 团队共享空间分离

### 标签管理
- 标签 CRUD（弱删除：移除可见性，保留关联）
- 标签颜色自定义
- 标签组归类
- 文件批量打标签
- 相似标签推荐（Levenshtein 编辑距离）
- 标签自动补全
- 多标签 AND 筛选

### 搜索
- Elasticsearch 全文搜索（可选，无 ES 时回退 MySQL LIKE）
- 按文件名、类型、空间、标签搜索
- 搜索建议、搜索历史
- 搜索模板（保存/加载/执行）
- 相似文件推荐
- 搜索结果高亮

### 回收站
- 文件/文件夹恢复
- 永久删除 / 批量删除 / 清空
- 30 天自动清理

### 统计概览
- 存储空间使用
- 文件类型分布饼图（ECharts）
- 最近文件列表
- 常用标签云
- 管理员全局统计

### AI 对话
- OpenAI 兼容接口（支持 OpenAI / 通义千问 / 智谱GLM / Claude / 本地部署）
- 多轮对话（对话历史保存）
- 文档引用（AI 基于文件内容回答）
- API Key AES-256 加密存储

### 系统管理
- 操作日志查看/清理
- 数据备份
- 全局 AI 配置

## 用户角色权限

| 操作 | 管理员 | 正式用户 | 访客 |
|---|---|---|---|
| 个人空间文件 | 全部 | 全部（仅自己的） | — |
| 团队空间文件 | 全部 | 下载/预览/加标签 | 仅预览/下载 |
| 用户管理 | ✅ | — | — |
| 系统设置 | ✅ | — | — |
| AI 对话 | ✅ | ✅（自行配置 Key） | — |

## 数据库表

14 张核心表：sys_user、file_info、folder、file_chunk、tag、user_tag_visible、file_tag、tag_group、tag_group_item、recycle_bin、search_history、search_template、ai_api_config、operation_log

## 开发阶段

| 阶段 | 内容 | 状态 |
|---|---|---|
| 第一阶段 | 项目搭建 + 用户管理 | ✅ |
| 第二阶段 | 文件管理核心 | ✅ |
| 第三阶段 | 标签管理 + 回收站 | ✅ |
| 第四阶段 | 搜索模块 | ✅ |
| 第五阶段 | 统计概览 + 系统管理 | ✅ |
| 第六阶段 | AI 模块 | ✅ |
| 第七阶段 | 部署 + 优化 + 文档 | 🔄 进行中 |

## 配置说明

关键配置项在 `src/main/resources/application.yml`：

```yaml
# 数据库
spring.datasource.url: jdbc:mysql://localhost:3306/teamfiles

# Redis
spring.data.redis.host: localhost

# Elasticsearch（可选）
spring.elasticsearch.uris: http://localhost:9200

# JWT
jwt.expiration: 1800        # 默认 30 分钟
jwt.remember-expiration: 604800  # 记住我 7 天

# 文件存储
file.storage.base-path: ./data/files
file.chunk-size: 5242880     # 5MB/片

# AI 加密密钥
ai.encrypt.secret-key: teamfiles-aes-key-256bit-padding!
```

## License

MIT

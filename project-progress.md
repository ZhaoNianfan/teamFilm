---
name: phase3-complete
description: TeamFiles 项目第三阶段（标签管理+回收站）已完成，即将进入第四阶段（搜索模块）
metadata: 
  node_type: memory
  type: project
  originSessionId: ef719a76-39b1-4525-96a2-a7b8f4e4e85f
---

TeamFiles 局域网团队文件管理系统，项目路径 `d:\Program\workspace1\filmTab`，包名 `com.myself.teamfiles`。

## 第一阶段已完成（基础框架+用户管理）
- 后端：Spring Boot 3.5.14 + Security + JWT + MyBatis-Plus，用户认证/管理模块
- 前端：Vue 3 + Element Plus + Pinia + Router，登录/改密码页
- 数据库：14 张表 DDL，admin/admin123 初始账号

## 第二阶段已完成（文件管理核心）
- 单文件/批量/分片上传、MD5 秒传、文件CRUD、文件夹树、预览/下载/ZIP
- 文件管理页面（列表/网格、文件夹树、拖拽上传、右键菜单）

## 第三阶段已完成（标签管理+回收站）— 2026-05-13

### 标签后端新增文件
- `module/tag/entity/` — Tag.java, FileTag.java, UserTagVisible.java, TagGroup.java, TagGroupItem.java（5个实体）
- `module/tag/mapper/` — TagMapper, FileTagMapper, UserTagVisibleMapper, TagGroupMapper, TagGroupItemMapper（5个Mapper）
- `module/tag/dto/` — TagDTO.java, TagVO.java, TagGroupDTO.java, BatchTagsDTO.java
- `module/tag/service/TagService.java` + `impl/TagServiceImpl.java` — 标签CRUD/文件关联/自动补全/相似推荐（Levenshtein）/标签组CRUD
- `module/tag/controller/TagController.java` — 标签+标签组全部 API（含文件标签关联、批量操作）

### 回收站后端新增文件
- `module/recycle/dto/` — RecycleVO.java, RecyclePageDTO.java
- `module/recycle/service/RecycleService.java` + `impl/RecycleServiceImpl.java` — 列表/恢复/永久删除/批量/清空
- `module/recycle/controller/RecycleController.java` — 回收站全部 API

### 后端修复
- `RecycleCleanTask.java` — 修复使用 basePath 前缀正确删除物理文件

### 前端新增/修改文件
- `src/api/tag.ts` — 标签+标签组 API
- `src/api/recycle.ts` — 回收站 API
- `src/components/tag/TagBadge.vue` — 彩色标签徽章组件
- `src/components/tag/TagInput.vue` — 标签输入（自动补全+相似警告）
- `src/views/tags/TagManagePage.vue` — 完整标签管理页（标签网格+标签组侧栏+增删改查）
- `src/views/recycle/RecycleBinPage.vue` — 完整回收站页（个人/团队切换、恢复/永久删除/清空、过期倒计时）

### 核心特性
- 标签弱删除：移除 user_tag_visible，保留 tag 和 file_tag 关联
- EditeLevenshtein 编辑距离 ≤2 的相似标签推荐
- 标签自动补全（前缀匹配）
- 标签组：创建/编辑/删除/添加移除标签（用户私有）
- 回收站：个人/团队分开、恢复（撤销逻辑删除）、永久删除（物理删除文件）、清空
- 30天自动清理定时任务（RecycleCleanTask 每日凌晨2点）
- 24h 分片自动清理（ChunkCleanTask 每日凌晨2:30）

## 下一步：第四阶段 - 搜索模块
- ES尚未部署，先基于 MySQL LIKE 实现 fallback 搜索
- ES 部署后通过策略模式无缝切换
- 需要实现：综合搜索、搜索建议、搜索历史、搜索模板、相似文件推荐

环境：MySQL(3306, root/123456)、Redis(6379, 无密码)、无 ES
启动：mvnw spring-boot:run (端口 8088)，前端 cd teamfiles-web && npm run dev

Why: 记录第三阶段完成的断点。
How to apply: 用户说"继续"或"继续第四阶段"时，从搜索模块开始开发。

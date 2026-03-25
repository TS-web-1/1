# 墨香书阁 - 小说阅读平台

一个基于 Spring Boot + Vue 3 的全栈小说阅读平台，支持用户阅读、作者创作、管理员审核等功能。

## 🚀 技术栈

### 后端
- **Spring Boot 3.2.6** - 核心框架
- **Spring Security** - 安全认证
- **Spring Data JPA** - 数据访问
- **MySQL** - 数据库
- **Redis** - 缓存
- **JWT** - 身份验证
- **Maven** - 构建工具

### 前端
- **Vue 3** - 前端框架
- **Vue Router 4** - 路由管理
- **Pinia** - 状态管理
- **Element Plus** - UI 组件库
- **Vite** - 构建工具
- **Axios** - HTTP 客户端

## 📁 项目结构

```
project/
├── novel-system/          # Spring Boot 后端
│   ├── src/main/java/com/novel/
│   │   ├── controller/    # REST API 控制器
│   │   ├── service/       # 业务逻辑层
│   │   ├── repository/    # 数据访问层
│   │   ├── model/         # 实体类
│   │   ├── dto/           # 数据传输对象
│   │   ├── exception/     # 异常处理
│   │   └── util/          # 工具类
│   └── src/test/          # 单元测试
│
├── vue-project/           # Vue 3 前端
│   ├── src/
│   │   ├── views/         # 页面组件
│   │   ├── components/    # 可复用组件
│   │   ├── api/           # API 接口
│   │   ├── stores/        # Pinia 状态管理
│   │   ├── router/        # 路由配置
│   │   └── utils/         # 工具函数
│   └── public/            # 静态资源
│
└── README.md
```

## 🛠️ 快速开始

### 环境要求
- Java 17+
- Node.js 20+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

```bash
cd novel-system
mvn spring-boot:run
```

后端服务将运行在 `http://localhost:8080/api`

### 前端启动

```bash
cd vue-project
npm install
npm run dev          # 用户端 (端口 8081)
npm run dev:author   # 作者端 (端口 8082)
npm run dev:admin    # 管理端 (端口 8083)
```

## 📦 功能特性

### 用户端
- ✅ 用户注册/登录
- ✅ 小说浏览和搜索
- ✅ 小说阅读和书签
- ✅ 评论和点赞
- ✅ 个性化推荐
- ✅ 阅读历史

### 作者端
- ✅ 作品管理
- ✅ 章节发布
- ✅ 草稿箱
- ✅ 数据统计
- ✅ 评论管理

### 管理端
- ✅ 用户管理
- ✅ 小说审核
- ✅ 章节审核
- ✅ 分类管理
- ✅ 数据统计

## 🔧 代码规范

### ESLint + Prettier

```bash
# 检查代码
npm run lint:check

# 自动修复
npm run lint

# 格式化代码
npm run format
```

### 提交代码

```bash
git add -A
git commit -m "描述信息"
git push origin main
```

## 🧪 单元测试

```bash
cd novel-system
mvn test
```

## 📈 性能优化

### 前端优化
- ✅ 路由懒加载
- ✅ 组件按需加载
- ✅ 图片懒加载
- ✅ 代码分割
- ✅ 资源预加载

### 后端优化
- ✅ 数据库索引
- ✅ Redis 缓存
- ✅ 分页查询
- ✅ 异常处理

## 🔐 安全特性

- JWT 身份认证
- 密码加密存储 (BCrypt)
- 角色权限控制
- SQL 注入防护
- XSS 防护

## 📝 API 文档

### 认证相关
- `POST /api/auth/login` - 用户登录
- `POST /api/auth/register` - 用户注册
- `GET /api/auth/me` - 获取当前用户

### 小说相关
- `GET /api/novels` - 获取小说列表
- `GET /api/novels/{id}` - 获取小说详情
- `POST /api/novels` - 创建小说
- `PUT /api/novels/{id}` - 更新小说
- `DELETE /api/novels/{id}` - 删除小说

### 更多 API
详见后端代码中的 Controller 层

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

## 📄 许可证

[MIT](LICENSE)

## 👨‍💻 作者

- **TS-web-1** - *Initial work*

## 🙏 致谢

- [Vue.js](https://vuejs.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Element Plus](https://element-plus.org/)

# API 接口文档

## 基础信息

- **Base URL**: `http://localhost:8080/api`
- **认证方式**: JWT Token (Bearer)
- **Content-Type**: `application/json`

## 认证相关

### 用户登录

```http
POST /auth/login
```

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "appType": "user"  // user, author, admin
}
```

**响应**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "userId": 1,
    "username": "testuser",
    "email": "test@example.com",
    "role": "USER"
  }
}
```

### 用户注册

```http
POST /auth/register
```

**请求体**:
```json
{
  "username": "string",
  "password": "string",
  "email": "string",
  "userType": "user"  // user, author
}
```

## 小说相关

### 获取小说列表

```http
GET /novels?page=0&size=24&category=&status=
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "content": [...],
    "totalElements": 100,
    "totalPages": 5,
    "currentPage": 0,
    "size": 24
  }
}
```

### 获取小说详情

```http
GET /novels/{id}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "title": "小说标题",
    "author": "作者名",
    "description": "简介",
    "category": "玄幻",
    "status": "ONGOING",
    "views": 1000,
    "bookmarks": 50,
    "totalLikes": 100
  }
}
```

### 搜索小说

```http
GET /novels/search?keyword=关键词
```

## 阅读相关

### 获取章节内容

```http
GET /reading/chapter/{chapterId}
```

### 保存阅读进度

```http
POST /reading/progress/{novelId}?chapterId=1&position=100&progressPercent=0.5&readTime=300
```

### 获取阅读进度

```http
GET /reading/progress/{novelId}
```

## 评论相关

### 发表评论

```http
POST /comments
```

**请求体**:
```json
{
  "novelId": 1,
  "chapterId": 1,
  "content": "评论内容",
  "commentType": "NOVEL",
  "parentId": null
}
```

### 获取小说评论

```http
GET /comments/novel/{novelId}?page=0&size=10&sortBy=createdAt
```

## 作者相关

### 获取作者作品

```http
GET /author/novels
```

### 创建小说

```http
POST /author/novels
```

**请求体**:
```json
{
  "title": "小说标题",
  "description": "简介",
  "category": "玄幻",
  "coverImage": "封面URL"
}
```

### 创建章节

```http
POST /author/novels/{novelId}/chapters
```

**请求体**:
```json
{
  "title": "章节标题",
  "content": "章节内容",
  "chapterNumber": 1
}
```

## 管理相关

### 获取待审核小说

```http
GET /admin/novels/pending?page=0&size=20
```

### 审核小说

```http
POST /admin/novels/{novelId}/review?status=APPROVED&reviewComment=审核意见
```

### 获取平台统计

```http
GET /admin/stats
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalUsers": 1000,
    "totalNovels": 500,
    "totalChapters": 5000,
    "totalViews": 100000
  }
}
```

## 错误码

| 状态码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 认证头

所有需要认证的接口都需要在请求头中添加：

```http
Authorization: Bearer {token}
```

# GitHubOAuth 后端服务

这是一个基于 **Spring Boot 3** 的后端示例工程，为前端 Bytebase 风格登录页提供 **GitHub OAuth 登录能力**，主要功能包括：

- 跳转到 GitHub 授权页面；
- 接收 GitHub 回调并通过 `code` 换取 `access_token`；
- 调用 GitHub API 获取当前登录用户信息（头像、昵称、login、邮箱等）；
- 使用 `HttpSession` 维护登录态，向前端提供当前用户信息接口；
- 提供退出登录接口，清除当前会话。

---

## 技术栈

- Spring Boot 3.5.x
- Spring Web MVC
- Spring WebFlux（用于 WebClient 调用 GitHub API）
- MyBatis / MySQL / Redis 等依赖目前不是必须，仅作为基础框架预置

---

## 主要接口

### 1. GitHub 登录入口

- **HTTP 方法**：`GET`
- **URL**：`/api/auth/github/login`
- **说明**：
  - 构造 GitHub 授权地址：`https://github.com/login/oauth/authorize`
  - 附带 `client_id`、`redirect_uri`、`scope` 等参数
  - 重定向到 GitHub 授权页面

### 2. GitHub 回调

- **HTTP 方法**：`GET`
- **URL**：`/api/auth/github/callback`
- **说明**：
  1. GitHub 授权成功后会携带 `code` 回调该接口；
  2. 后端使用 `code` 调用 `https://github.com/login/oauth/access_token` 换取 `access_token`；
  3. 使用 `access_token` 调用 `https://api.github.com/user` 获取用户信息；
  4. 将用户信息封装为 `GithubUserDto` 并写入 `HttpSession`；
  5. 最后重定向回前端，例如 `http://localhost:3000/?login=success`。

### 3. 获取当前登录用户

- **HTTP 方法**：`GET`
- **URL**：`/api/auth/me`
- **说明**：
  - 从 `HttpSession` 中读取当前 GitHub 用户信息；
  - 登录状态正常时返回 `200 + GithubUserDto`；
  - 未登录或 session 中没有用户信息时返回 `401`。

### 4. 退出登录

- **HTTP 方法**：`POST`
- **URL**：`/api/auth/logout`
- **说明**：
  - 调用 `session.invalidate()` 清除会话；
  - 返回 `204 No Content`；
  - 前端可据此清空本地用户状态并重新发起授权。

---

## 配置说明

主配置文件：`src/main/resources/application.properties`

关键配置片段：

```properties
# 应用基础配置
spring.application.name=GitHubOAuth
server.port=4000

# GitHub OAuth 配置（提交代码时请不要填真实值）
# 换成自己的 ID 与秘钥，仅在本地或安全环境中配置真实值
app.github.client-id=YOUR_GITHUB_CLIENT_ID
app.github.client-secret=YOUR_GITHUB_CLIENT_SECRET
app.github.callback-url=http://localhost:4000/api/auth/github/callback

# 前端地址（用于回调成功后重定向）
frontend.url=http://localhost:3000
```

> **重要：不要将真实的 Client Secret 提交到公开仓库。**
> 建议在本地通过环境变量或单独的未提交配置文件注入真实值。

### GitHub OAuth App 配置

在 GitHub 上创建 OAuth App 时，建议配置为：

- **Homepage URL**：`http://localhost:3000`
- **Authorization callback URL**：`http://localhost:4000/api/auth/github/callback`

生成的 `Client ID` 和 `Client Secret` 对应上面的 `app.github.client-id` / `app.github.client-secret`。

---

## CORS 配置

本项目在 `cn.edu.ccibe.githuboauth.config.CorsConfig` 中开启了跨域，允许前端 `http://localhost:3000` 携带 Cookie 访问：

```java
registry.addMapping("/api/**")
        .allowedOrigins("http://localhost:3000")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowCredentials(true);
```

---

## 本地运行

1. 确保本地已安装 JDK 21+ 与 Maven。
2. 在本地安全环境中为 `app.github.client-id`、`app.github.client-secret` 配置真实值。
3. 在项目根目录 `GitHubOAuth` 运行：

```bash
mvn spring-boot:run
```

4. 服务启动后监听：`http://localhost:4000`
5. 前端（`login-page`）运行在 `http://localhost:3000`，通过 `/api/auth/...` 与本服务交互。

---

## 与前端的配合（login-page）

典型登录流程：

1. 前端用户点击 “Continue with GitHub” 按钮：
   - 跳转到 `http://localhost:4000/api/auth/github/login`
2. 后端：
   - 重定向到 GitHub 授权页；
   - 授权成功后 GitHub 回调 `/api/auth/github/callback?code=...`；
   - 换取 token + 获取用户信息，写入 session；
   - 重定向回前端 `http://localhost:3000/?login=success`。
3. 前端检测到 `login=success` 后：
   - 调用 `GET /api/auth/me` 获取当前登录用户信息并展示。
4. 用户点击 “退出登录 / 切换账号” 按钮：
   - 前端调用 `POST /api/auth/logout`，后端清除 session；
   - 下次需要重新走 GitHub 授权流程。

---

## 注意事项

- 当前项目中 MyBatis / MySQL / Redis 等配置为基础模板，可按需启用或删除；
- 若在公司/校园网环境中访问 GitHub API 可能需要配置 HTTP/HTTPS 代理；
- 生产环境请务必：
  - 使用 HTTPS 和可信回调地址；
  - 使用更安全的 session / token 存储方案；
  - 不要将敏感配置提交到代码仓库。

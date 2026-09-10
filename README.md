# Mall Service

基于 JDK 11 和 Spring Boot 2.7.18 的商城后端基础框架，集成 MyBatis、MySQL 8、Druid、Flyway、Redis、Spring Security、JWT 和 OpenAPI。

## 环境要求

- JDK 11
- Maven 3.5+
- MySQL 8
- Redis 6.2+

本项目只定义 GET、POST 业务接口。PUT、DELETE、PATCH、HEAD、OPTIONS 等请求统一返回 405；路由禁止使用 `{id}` 等路径变量。由于 OPTIONS 也被禁止，浏览器端必须通过开发服务器或 Nginx 将 `/api` 反向代理为同源请求。

## 本地配置

创建数据库：

```sql
CREATE DATABASE mall_service
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

设置环境变量。Redis 本地密码按当前环境使用 `123456`，JWT 密钥必须是 Base64 编码且解码后不少于 32 字节：

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/mall_service?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='你的 MySQL 密码'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export REDIS_PASSWORD='123456'
export JWT_SECRET="$(openssl rand -base64 32)"
```

启动服务：

```bash
mvn spring-boot:run
```

Flyway 会在首次启动时自动创建 `sys_user` 表。Swagger UI 地址为 <http://127.0.0.1:8080/swagger-ui.html>。

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 注册用户 |
| POST | `/api/auth/login` | 登录并创建独立设备会话 |
| POST | `/api/auth/refresh` | 轮换 Refresh Token |
| POST | `/api/auth/logout` | 注销当前设备会话 |
| GET | `/api/users/me` | 查询当前用户 |

注册：

```bash
curl -i -X POST 'http://127.0.0.1:8080/api/auth/register' \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}'
```

登录：

```bash
curl -i -X POST 'http://127.0.0.1:8080/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"alice","password":"password123"}'
```

使用登录响应中的 Access Token：

```bash
curl -i 'http://127.0.0.1:8080/api/users/me' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

刷新 Token：

```bash
curl -i -X POST 'http://127.0.0.1:8080/api/auth/refresh' \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"REFRESH_TOKEN"}'
```

退出：

```bash
curl -i -X POST 'http://127.0.0.1:8080/api/auth/logout' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

## 测试与构建

自动化测试不依赖真实 MySQL 或 Redis：

```bash
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml test
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml package
```

上述 `settings.xml` 使用 Aliyun Maven 镜像；若其他开发机已在默认 Maven 配置中设置镜像，可省略 `-s` 参数。

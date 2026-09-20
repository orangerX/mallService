# Mall Service

基于 JDK 11 和 Spring Boot 2.7.18 的 Maven 多模块水果商城后端。商城端和内管端是两个独立 Spring Boot 服务，共享 MyBatis 领域模块、MySQL 8 数据库、Druid、Flyway 与 Redis 基础设施。

## 模块结构

| 模块 | 职责 |
| --- | --- |
| `mall-common` | 统一响应、异常、分页、GET/POST 请求方法策略 |
| `mall-member` | 商城用户、收货地址、会员查询 |
| `mall-product` | Banner、分类、商品、SKU 与库存查询 |
| `mall-promotion` | 满减券模板、用户优惠券与核销 |
| `mall-order` | 购物车、下单、库存扣减、订单查询 |
| `mall-exam` | 学位英语题源、题库、蓝图、组卷、答题、判分与复盘 |
| `mall-database` | Flyway V1–V13 数据库迁移，包含考试领域表、启用的蓝图与五套原创模拟题种子 |
| `mall-shop-app` | 商城端与考生 API 应用，默认端口 `8080` |
| `mall-admin-app` | 内管端应用，提供商城管理和考试内容管理，默认端口 `8081` |

领域模块不依赖应用模块；两个应用各自持有 Controller、安全配置和启动类。`mallPage` 继续使用 `http://127.0.0.1:8080`，原有接口契约无需修改。

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

按实际环境设置下列变量；两个应用的配置位于各自的 `src/main/resources/application.yml`。仓库中的示例不代表可用账号或生产凭据，密码和密钥应由运行环境提供。

| 变量 | 配置要求 |
| --- | --- |
| `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` | 两个应用连接同一 MySQL 数据库；默认地址是本机 `3306/mall_service`，账号和密码按实际数据库配置 |
| `REDIS_HOST`、`REDIS_PORT`、`REDIS_DATABASE` | 两个应用使用同一 Redis 环境；默认分别为 `127.0.0.1`、`6379`、`0` |
| `REDIS_PASSWORD` | 两个应用均无默认值，需显式提供与 Redis 配置一致的值 |
| `JWT_SECRET` | 考生 / 商城应用使用，Base64 编码且解码后不少于 32 字节 |
| `ADMIN_JWT_SECRET` | 管理应用使用，Base64 编码且解码后不少于 32 字节 |
| `ADMIN_USERNAME`、`ADMIN_PASSWORD` | 管理员表为空时用于初始化管理员，使用自行配置的账号和密码 |
| `SERVER_PORT` | 两个应用分别默认为 `8080`、`8081`；如覆盖，必须按进程分别设置并同步调整代理目标 |

首次启动内管端且 `sys_admin` 为空时，必须提供 `ADMIN_USERNAME` 与 `ADMIN_PASSWORD`；密码只在启动时 BCrypt 加密后入库，源码和迁移中不保存明文。已有管理员后可省略这两个变量。商城 JWT audience 为 `mall-shop`、Redis 键前缀为 `mall:auth:session:`；内管 JWT audience 为 `mall-admin`、Redis 键前缀为 `mall:admin:session:`，两类 Token 不可互用。

以下命令供后续运行环境使用，本次文档任务未执行打包或启动。先在后端根目录打包，再在两个独立终端中分别启动已有 JAR，避免第一个前台进程阻塞第二个服务：

```bash
mvn package
java -jar mall-shop-app/target/mall-shop-app-1.0.0.jar
java -jar mall-admin-app/target/mall-admin-app-1.0.0.jar
```

Flyway 会集中校验历史迁移，V11 创建管理员表，V12 创建考试领域表，V13 从 `mall-database/src/main/resources/db/seed/degree-english-v1/` 导入五套原创模拟题并创建启用的蓝图。两个应用均加载同一迁移模块。商城 Swagger UI 地址为 <http://127.0.0.1:8080/swagger-ui.html>，内管 Swagger UI 地址为 <http://127.0.0.1:8081/swagger-ui.html>。

### 三仓库本地启动顺序与代理

1. 准备 MySQL 数据库和 Redis，向两个后端进程提供上述环境变量。MySQL 保存试卷、答案和历史；Redis 用于登录会话和并发组卷锁，两个依赖均需可用。
2. 启动 `mall-shop-app`（默认 `8080`），等待应用与 Flyway 迁移完成；再启动 `mall-admin-app`（默认 `8081`），空管理员表需先提供初始化变量。两个服务必须持续运行。
3. 在 `examPage` 的实际 checkout 中准备其 `package.json` 所需依赖，再运行 `npm run dev`；Node.js 要求为 `>=20.10.0`，Vite 端口固定为 `5174`，占用时因 `strictPort: true` 而退出。
4. 在 `mallManagePage` 的实际 checkout 中准备其 `package.json` 所需依赖，再运行 `npm run dev`；Node.js 同样要求 `>=20.10.0`，Vite 配置端口为 `5173`，未设置 `strictPort`，浏览器地址以终端输出为准。

两个 Vite 应用需在各自终端中持续运行。浏览器从对应前端地址进入，使用考生 / 商城账号或管理员账号登录；两类会话不可互换。以下对应关系来自两个前端现有的 `vite.config.ts` 和 `src/services/http.ts`，没有新增代理设置：

| 前端 | 本地入口 | 浏览器请求前缀 | Vite 代理目标 |
| --- | --- | --- | --- |
| `examPage` | `http://localhost:5174` | `/api` | `http://127.0.0.1:8080` |
| `mallManagePage` | `http://localhost:5173`（以实际输出为准） | `/admin/api` | `http://127.0.0.1:8081` |
| `mallManagePage` 商品图片 | 同管理端入口 | `/images` | `http://127.0.0.1:8081` |

代理不重写路径：例如 `/api/exams/current` 仍以完整路径交给 `8080`；`/admin/api/exam/records` 仍以完整路径交给 `8081`。不要删除 `/api` 或 `/admin/api` 前缀。

### 生产同源 HTTPS 反向代理

生产入口必须提供 HTTPS，并由 Web 服务器或网关在每个前端自己的同一域名、协议和端口下转发 API。考生站点的 `/api` 指向考生 / 商城服务 `8080`；管理站点的 `/admin/api` 指向管理服务 `8081`，管理图片 `/images` 也需有相应的服务路由。两个站点可以使用不同域名；“同源”要求是各自页面与自己的 API 同源。

网关必须保留完整 API 路径、查询参数、请求体与 `Authorization`，并正确传递原始 HTTPS 协议和主机信息；两个 Spring Boot 应用已配置 `forward-headers-strategy: framework`。导入接口使用 multipart，管理应用的上传限制为单文件 `5MB`、整次请求 `6MB`，代理限制需与其协调。

两个前端均使用无子路径参数的 `createWebHistory()`，适合分别挂载于各自站点根路径。页面路由刷新需要回退到对应前端的 `index.html`；API 与图片路由应优先匹配，不能被页面回退吞掉。把两个前端合并到同一域名的不同子路径需要另行设计并修改基础路径，当前配置未提供这种部署方式。

`vite.config.ts` 中的 `server.proxy` 是本地开发配置；发布静态资源后仍需独立配置生产网关。后端拒绝 OPTIONS，不能依赖跨域预检直接访问服务端口。本仓库未提供或执行本次生产网关配置、部署及连通性验证。

### Task 13 验收状态：按要求未执行

本次仅更新运行文档；未创建 Playwright 配置或 E2E / 测试文件，未安装依赖、启动服务、执行测试、类型检查、lint、构建、全量 Maven 验证、视觉验收或代码审查，也未部署。下列计划中的验收矩阵全部刻意保留为未执行，不代表通过或失败：

| 范围 | 原计划命令 / 验收内容 | 本次状态 |
| --- | --- | --- |
| `mallService` | `mvn test`、`mvn package -DskipTests` | 未执行 |
| `examPage` | `npm run typecheck`、`npm run lint`、`npm run test`、`npm run build`、`npm run test:e2e` | 未执行；当前 `package.json` 没有 `lint`、`test`、`test:e2e` 脚本 |
| `mallManagePage` | `npm run typecheck`、`npm run lint`、`npm run test`、`npm run build` | 未执行 |
| 完整考试流程 | 登录、组卷、保存、刷新恢复、交卷、复盘、自评；进行中响应无敏感答案字段 | 未执行 |
| 桌面布局 | 1024、1280、1440、1920px 下无横向滚动且提交按钮可见 | 未执行 |
| 设计规格 §13.4 | 十项功能、数据、权限与质量验收标准 | 全部未执行，未取得运行验收结论 |

这些是验收记录，不是本次执行指令。考生端和管理端各自的 README 也记录了本地启动与生产代理要求。

## 内管端接口

内管端所有业务接口都需要管理员 Access Token（登录和刷新除外），分页默认 `page=1&size=20`，且 `size` 最大为 100。商品售价和库存均在 SKU 维度维护，划线价由服务端按售价的 120% 自动计算。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/admin/api/auth/login` | 管理员登录 |
| POST | `/admin/api/auth/refresh` | 轮换管理员 Refresh Token |
| POST | `/admin/api/auth/logout` | 注销当前管理员会话 |
| GET | `/admin/api/auth/me` | 当前管理员信息 |
| GET | `/admin/api/categories` | 类目列表 |
| POST | `/admin/api/categories/create` | 新增类目，JSON Body 传名称、编码、排序和状态 |
| POST | `/admin/api/categories/update` | 编辑类目，JSON Body 传 `categoryId` 及完整类目信息 |
| POST | `/admin/api/categories/status` | 启用或停用类目 |
| POST | `/admin/api/categories/delete` | 删除无关联商品的类目 |
| GET | `/admin/api/products` | 按 `keyword`、`categoryId`、`status` 分页查询商品 |
| GET | `/admin/api/products/detail?productId=1` | 商品及全部 SKU、价格和库存 |
| POST | `/admin/api/products/create` | 新增商品并一次创建 1–20 个 SKU |
| POST | `/admin/api/products/update` | 编辑商品基础信息和所属类目 |
| POST | `/admin/api/products/status` | 商品上架或下架 |
| POST | `/admin/api/products/delete` | 删除无历史订单的商品 |
| POST | `/admin/api/products/skus/create` | 为商品新增 SKU |
| POST | `/admin/api/products/skus/update` | 编辑 SKU 价格、库存、名称等信息 |
| POST | `/admin/api/products/skus/status` | SKU 上架或下架 |
| POST | `/admin/api/products/skus/set-default` | 设置商品默认 SKU |
| POST | `/admin/api/products/skus/delete` | 删除非末尾且无历史订单的 SKU |
| GET | `/admin/api/orders` | 按 `orderNo`、`userId`、`status` 分页查询订单 |
| GET | `/admin/api/orders/detail?orderId=1` | 订单商品、SKU、收货和优惠详情 |
| GET | `/admin/api/users` | 按 `keyword`、`status` 分页查询商城用户 |
| GET | `/admin/api/users/detail?userId=1` | 查询商城用户详情 |
| POST | `/admin/api/users/create` | 新增商城用户，默认密码为 `123456` |
| POST | `/admin/api/users/update` | 编辑商城用户的用户名和状态 |
| POST | `/admin/api/users/status` | 启用或禁用商城用户 |
| POST | `/admin/api/users/reset-password` | 将商城用户密码重置为 `123456` |
| POST | `/admin/api/users/delete` | 删除无历史订单的商城用户 |
| GET | `/admin/api/coupons` | 按 `name`、`status` 分页查询券模板 |
| GET | `/admin/api/coupons/detail?couponId=1` | 查询优惠券模板详情 |
| POST | `/admin/api/coupons/create` | 新增满减券模板 |
| POST | `/admin/api/coupons/update` | 编辑满减券模板 |
| POST | `/admin/api/coupons/status` | 启用或停用优惠券 |
| POST | `/admin/api/coupons/delete` | 删除无领取记录的优惠券 |

考试管理另提供以下 16 个接口，全部使用管理员 Access Token；分页接口同样使用 `page=1&size=20`，`page >= 1`、`1 <= size <= 100`：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/admin/api/exam/sources` | 按关键词、来源类型、版权、审核及启停状态分页查询题源 |
| POST | `/admin/api/exam/sources/create` | 新增题源 |
| POST | `/admin/api/exam/sources/update` | 修改题源，JSON Body 传 `sourceId` |
| POST | `/admin/api/exam/sources/review` | 通过或驳回题源，并记录版权状态和审核意见 |
| POST | `/admin/api/exam/sources/status` | 启用或停用题源 |
| GET | `/admin/api/exam/questions` | 按题型、审核/启停状态、题源、考点和关键词分页查询题库 |
| GET | `/admin/api/exam/questions/detail?questionId=1` | 获取单题或所属完整题组 |
| POST | `/admin/api/exam/questions/create` | 新建单题或完整对话、阅读题组 |
| POST | `/admin/api/exam/questions/update` | 修改尚未发布的单题或完整题组 |
| POST | `/admin/api/exam/questions/review` | 审核发布或驳回题目，题组整体处理 |
| POST | `/admin/api/exam/questions/status` | 启用或停用单题或完整题组 |
| POST | `/admin/api/exam/imports/preview` | multipart 上传 `sourceId` 和 `file`，预览 JSON/CSV 校验结果；文件不超过 5 MiB |
| POST | `/admin/api/exam/imports/commit` | JSON Body 传 `batchId`，将已确认批次写入草稿题库 |
| GET | `/admin/api/exam/blueprints` | 查看蓝图、部分结构与可用题库容量 |
| POST | `/admin/api/exam/blueprints/status` | JSON Body 传 `blueprintId`、`status`，启用或停用蓝图 |
| GET | `/admin/api/exam/records` | 按 `userId`、`paperNo`、`status` 分页查询考试记录，不返回答案正文 |

```bash
curl -X POST 'http://127.0.0.1:8081/admin/api/auth/login' \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"123456"}'

curl 'http://127.0.0.1:8081/admin/api/products?page=1&size=20' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN'

curl -X POST 'http://127.0.0.1:8081/admin/api/products/create' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"categoryId":1,"name":"冰糖心苹果","subtitle":"清甜爽脆","imagePath":"/images/products/seasonal-fruit-placeholder.jpg","sortOrder":10,"status":1,"skus":[{"skuCode":"APPLE-1KG","skuName":"1kg装","price":19.90,"unit":"1kg/份","stock":100,"defaultSku":true,"sortOrder":1,"status":1}]}'
```

商品创建时必须至少提供一个 SKU，最多 20 个；未指定默认 SKU 时会自动选择第一个在售 SKU。SKU 编码全局唯一，同一商品内 SKU 名称唯一。修改默认 SKU 时应将 `defaultSku` 保持为 `true`，或先调用 `set-default` 切换默认规格。已有历史订单的商品或 SKU 不允许物理删除，商品也不允许删除最后一个 SKU。

订单列表支持按订单号模糊查询，并可结合用户 ID、订单状态和分页参数筛选。详情返回下单时保存的商品与 SKU 快照、数量、成交价、划线价、优惠券、实付金额和完整收货信息；商品图片路径会转换为可直接访问的绝对 URL：

```bash
curl 'http://127.0.0.1:8081/admin/api/orders?orderNo=202609&userId=1&status=1&page=1&size=20' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN'

curl 'http://127.0.0.1:8081/admin/api/orders/detail?orderId=1' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN'
```

商城用户的用户名仅支持 4–32 位字母、数字和下划线。新建用户与密码重置统一使用默认密码 `123456`；存在历史订单的用户不能物理删除，应改为禁用：

```bash
curl -X POST 'http://127.0.0.1:8081/admin/api/users/create' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"username":"orange_user","status":1}'

curl -X POST 'http://127.0.0.1:8081/admin/api/users/reset-password' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"userId":1}'
```

内管优惠券目前支持满减类型，优惠金额必须小于满减门槛，发行数量不能低于已领取数量，结束时间必须晚于开始时间。优惠券一旦被领取，满减门槛、优惠金额及开始时间即锁定；仍可调整名称、结束时间、发行数量、排序及启停状态。存在领取记录的优惠券不能物理删除：

```bash
curl -X POST 'http://127.0.0.1:8081/admin/api/coupons/create' \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"name":"周末鲜果券","thresholdAmount":100,"discountAmount":10,"totalQuantity":500,"startAt":"2026-09-01T00:00:00","endAt":"2026-10-01T00:00:00","status":1,"sortOrder":1}'
```

## 接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/about` | 获取商城名称、描述和版本号，无需登录 |
| POST | `/api/auth/register` | 注册用户 |
| POST | `/api/auth/login` | 登录并创建独立设备会话 |
| POST | `/api/auth/refresh` | 轮换 Refresh Token |
| POST | `/api/auth/logout` | 注销当前设备会话 |
| GET | `/api/users/me` | 查询当前用户 |
| GET | `/api/addresses` | 查询当前用户的收货地址 |
| GET | `/api/addresses/default` | 查询默认收货地址 |
| POST | `/api/addresses/create` | 新增收货地址 |
| POST | `/api/addresses/update` | 修改收货地址 |
| POST | `/api/addresses/delete` | 删除收货地址 |
| POST | `/api/addresses/set-default` | 设置默认收货地址 |
| GET | `/api/banners/home` | 获取首页 3 张时令水果 Banner，无需登录 |
| GET | `/api/categories/home` | 获取首页商品分类，无需登录 |
| GET | `/api/products?categoryId=1` | 按分类获取商品及 SKU，无需登录 |
| GET | `/api/coupons/available` | 查询当前可领取的满减券，无需登录 |
| POST | `/api/coupons/receive` | 领取满减券 |
| GET | `/api/coupons/mine?status=1` | 查询当前用户优惠券，可按状态筛选 |
| GET | `/api/cart` | 查询当前用户购物车 |
| POST | `/api/cart/items/add` | 添加商品或累加数量 |
| POST | `/api/cart/items/update` | 修改商品数量 |
| POST | `/api/cart/items/remove` | 移除单个商品 |
| POST | `/api/cart/clear` | 清空当前用户购物车 |
| POST | `/api/orders/submit` | 将当前购物车全部商品提交为订单 |
| GET | `/api/orders` | 查询当前用户订单列表 |
| GET | `/api/orders/detail?orderId=1` | 查询当前用户订单详情 |

### 学位英语考生接口

以下八个接口均要求商城用户 Access Token：`Authorization: Bearer ACCESS_TOKEN`。考生身份只取自登录上下文，请求不接受 `userId`；管理员 Token 不可用于考生接口。成功结果使用 `ApiResponse`，历史分页的 `data` 使用 `PageResponse`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/exams/generate` | 无需请求体，生成或恢复当前试卷 |
| GET | `/api/exams/current` | 当前试卷、已保存答案、服务端时间和截止时间；无当前考试时 `data` 为 `null` |
| POST | `/api/exams/answers/save` | JSON Body 传 `paperId`、`paperItemId`、`answerVersion`、`answerContent`，保存单题答案 |
| POST | `/api/exams/submit` | JSON Body 只需 `paperId`，提前交卷或由服务端判定超时交卷，重复提交幂等返回结果 |
| GET | `/api/exams/records` | 当前用户考试历史，默认 `page=1&size=20`，`page >= 1`、`1 <= size <= 100` |
| GET | `/api/exams/review?paperId=1` | 交卷后读取错题、未答客观题解析与主观题参考材料 |
| POST | `/api/exams/self-score` | JSON Body 传 `paperId`、`translationScore`、`writingScore`，两项分数均为 0–15 的整数 |
| GET | `/api/exams/wrong-summary` | 当前用户历次已提交考试的错题及未答客观题数量，按题型和考点汇总 |

考试使用 `DEGREE_ENGLISH_2016_V2` 蓝图，时长固定为 120 分钟，截止时间以服务端为准。每位用户同时只有一场进行中的考试；读取、保存或提交发现超时后，服务端完成自动交卷。前端倒计时归零时仍发送 `{"paperId":1}`，无需传 `automatic` 或剩余时长。交卷后客观题满分为 70 分；翻译、写作各自评 0–15 分，自评总分满分 100 分。

生成和当前考试响应只包含试题及考生自己的已保存答案，不包含 `correctAnswer`、`explanation`、`referenceAnswer`、`sampleEssay`、`sampleAnswer` 或 `scoringRubric`。复盘只展示答错/未答的客观题和翻译、写作参考材料。历史列表只含状态、时间与成绩；错题汇总返回 `totalWrong`、`byQuestionType`、`byKnowledgePoint`、`serverTime`，同一错题重复的考点标签只计一次。

首次保存的 `answerVersion` 为 `0`，成功后使用响应中的递增版本。版本冲突返回 HTTP 409、`code=EXAM_ANSWER_CONFLICT`，并在 `data.latestAnswer` 返回该考生的服务端答案及最新版本。保存请求到达时已超时会返回 `EXAM_EXPIRED`，前端使用同一 `paperId` 再次调用提交接口取得已完成的结果；交卷幂等，不重复判分或写入错题。

```bash
curl -X POST 'http://127.0.0.1:8080/api/exams/generate' \
  -H 'Authorization: Bearer ACCESS_TOKEN'

curl -X POST 'http://127.0.0.1:8080/api/exams/answers/save' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"paperId":1,"paperItemId":1,"answerVersion":0,"answerContent":"B"}'

curl -X POST 'http://127.0.0.1:8080/api/exams/submit' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"paperId":1}'
```

考试端浏览器通过开发服务器或 Nginx 将 `/api` 同源代理到商城服务 `8080`；管理端将 `/admin/api` 代理到内管服务 `8081`，代理须保留 `Authorization`。后端禁止 OPTIONS，不能依赖跨域预检直连。MySQL 保存试卷、答案和历史，Redis 同时用于登录会话和并发组卷锁，两个依赖均需运行。

初始内容定位为五套容量的原创模拟题（`ORIGINAL_SIMULATION`），不是官方历年真题。V12 创建考试领域结构，V13 创建启用的 `DEGREE_ENGLISH_2016_V2` 蓝图，并导入已审核、启用的原创题源和五套种子内容；本次未运行迁移或容量验收。后续通过管理端导入提交的内容只生成草稿，题源和题目仍需审核发布；容量不足返回 `EXAM_BANK_INSUFFICIENT`。

关于接口用于展示商城基础信息，当前版本为 `1.0.0`，商城主要售卖新鲜、优质的时令水果：

```bash
curl 'http://127.0.0.1:8080/api/about'
```

首页 Banner 接口返回绝对图片 URL，可直接赋值给小程序 `swiper` 中的 `image` 组件：

```bash
curl 'http://127.0.0.1:8080/api/banners/home'
```

获取首页分类和对应商品：

```bash
curl 'http://127.0.0.1:8080/api/categories/home'
curl 'http://127.0.0.1:8080/api/products?categoryId=1'
```

初始化数据包含“新鲜热卖”“甜蜜果园”“缤纷莓果”“热带鲜享”“柑橘飘香”5 个分类；后4个分类各包含2个商品并共用一张时令水果占位图。每个商品包含3个重量 SKU，重量组合按商品随机分为 `500g/1kg/2.5kg`、`400g/800g/2kg` 或 `750g/1.5kg/3kg`。商品接口中的 `skus` 返回各规格独立的价格、划线价、库存和销量，顶层价格字段对应 `defaultSkuId` 指向的默认规格。`imageUrl` 是可供小程序直接使用的绝对地址，`originalPrice` 固定比正常售价 `price` 高 20%。实际调用时应从分类接口读取 `id`，并通过查询参数 `categoryId` 传给商品接口；接口不使用路径变量。

本地微信开发者工具需要关闭“校验合法域名”；生产环境需使用 HTTPS，并在小程序后台将 API 域名加入 `request` 和 `downloadFile` 合法域名。反向代理应正确传递 `Host`、`X-Forwarded-Host` 和 `X-Forwarded-Proto`，以便接口生成公网图片地址。

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

收货地址接口均需携带 Access Token。首个地址会自动成为默认地址，设置新默认地址时会自动取消原默认地址：

```bash
curl -X POST 'http://127.0.0.1:8080/api/addresses/create' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"receiverName":"张三","receiverPhone":"13800138000","province":"上海市","city":"上海市","district":"浦东新区","detailAddress":"示例路1号","addressLabel":"家","defaultAddress":true}'

curl 'http://127.0.0.1:8080/api/addresses' \
  -H 'Authorization: Bearer ACCESS_TOKEN'

curl -X POST 'http://127.0.0.1:8080/api/addresses/update' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"addressId":1,"receiverName":"张三","receiverPhone":"13800138000","province":"上海市","city":"上海市","district":"浦东新区","detailAddress":"示例路2号","addressLabel":"家"}'

curl -X POST 'http://127.0.0.1:8080/api/addresses/set-default' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"addressId":1}'

curl -X POST 'http://127.0.0.1:8080/api/addresses/delete' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"addressId":1}'
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

购物车接口均需携带 Access Token。购物车以 SKU 为最小单位，`skuId` 从商品接口的 `skus` 数组获取；商品数量必须为 1–99，且不能超过该 SKU 的实时库存：

```bash
curl -X POST 'http://127.0.0.1:8080/api/cart/items/add' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"skuId":2,"quantity":2}'

curl 'http://127.0.0.1:8080/api/cart' \
  -H 'Authorization: Bearer ACCESS_TOKEN'

curl -X POST 'http://127.0.0.1:8080/api/cart/items/update' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"skuId":2,"quantity":3}'

curl -X POST 'http://127.0.0.1:8080/api/cart/items/remove' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"skuId":2}'

curl -X POST 'http://127.0.0.1:8080/api/cart/clear' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

优惠券支持满减模式，初始化数据包含满 `49` 减 `5`、满 `99` 减 `15`、满 `199` 减 `35` 三张券。每位用户每种券限领一张，领取与下单核销都使用数据库事务和并发校验：

```bash
curl 'http://127.0.0.1:8080/api/coupons/available'

curl -X POST 'http://127.0.0.1:8080/api/coupons/receive' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"couponId":1}'

curl 'http://127.0.0.1:8080/api/coupons/mine?status=1' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

用户优惠券状态为 `1` 未使用、`2` 已使用、`3` 已过期。下单时可传入“我的优惠券”接口返回的 `userCouponId`；不使用优惠券时省略该字段。订单响应中的 `totalAmount` 为商品总额、`couponDiscountAmount` 为满减金额、`paymentAmount` 为实付金额。

提交订单时会校验商品及 SKU 状态和 SKU 库存，在一个事务中保存商品与 SKU 快照、扣减 SKU 库存、增加 SKU 销量并清空购物车。新订单状态为“待支付”：

```bash
curl -X POST 'http://127.0.0.1:8080/api/orders/submit' \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"receiverName":"张三","receiverPhone":"13800138000","receiverAddress":"上海市浦东新区示例路1号","remark":"送达前联系","userCouponId":1}'

curl 'http://127.0.0.1:8080/api/orders' \
  -H 'Authorization: Bearer ACCESS_TOKEN'

curl 'http://127.0.0.1:8080/api/orders/detail?orderId=1' \
  -H 'Authorization: Bearer ACCESS_TOKEN'
```

## 测试与构建

自动化测试不依赖真实 MySQL 或 Redis：

```bash
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml test
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml package
```

上述 `settings.xml` 使用 Aliyun Maven 镜像；若其他开发机已在默认 Maven 配置中设置镜像，可省略 `-s` 参数。

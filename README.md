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
| `mall-database` | Flyway V1–V11 数据库迁移 |
| `mall-shop-app` | 商城端应用，默认端口 `8080` |
| `mall-admin-app` | 内管端应用，提供用户、类目、商品、SKU、订单和优惠券管理，默认端口 `8081` |

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

设置环境变量。Redis 本地密码按当前环境使用 `123456`，JWT 密钥必须是 Base64 编码且解码后不少于 32 字节：

```bash
export DB_URL='jdbc:mysql://127.0.0.1:3306/mall_service?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true'
export DB_USERNAME='root'
export DB_PASSWORD='123456'
export REDIS_HOST='127.0.0.1'
export REDIS_PORT='6379'
export REDIS_PASSWORD='123456'
export JWT_SECRET="$(openssl rand -base64 32)"
export ADMIN_JWT_SECRET="$(openssl rand -base64 32)"
export ADMIN_USERNAME='admin'
export ADMIN_PASSWORD='123456'
```

首次启动内管端且 `sys_admin` 为空时，必须提供 `ADMIN_USERNAME` 与 `ADMIN_PASSWORD`；密码只在启动时 BCrypt 加密后入库，源码和迁移中不保存明文。已有管理员后可省略这两个变量。商城 JWT audience 为 `mall-shop`、Redis 键前缀为 `mall:auth:session:`；内管 JWT audience 为 `mall-admin`、Redis 键前缀为 `mall:admin:session:`，两类 Token 不可互用。

在根目录打包后分别启动两个服务：

```bash
mvn package
java -jar mall-shop-app/target/mall-shop-app-1.0.0.jar
java -jar mall-admin-app/target/mall-admin-app-1.0.0.jar
```

Flyway 会集中校验 V1–V10，并通过 V11 创建管理员表。商城 Swagger UI 地址为 <http://127.0.0.1:8080/swagger-ui.html>，内管 Swagger UI 地址为 <http://127.0.0.1:8081/swagger-ui.html>。

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

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

Flyway 会在首次启动时自动创建用户、Banner、商品分类、商品和购物车表，并写入首页演示数据。Swagger UI 地址为 <http://127.0.0.1:8080/swagger-ui.html>。

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

# 商城售后功能实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有商城端、内管端和两个前端中实现订单履约、“仅退款”和“退货退款”的完整可运行闭环。

**Architecture:** 售后作为 `mall-order` 中独立聚合，通过 V12 增加订单履约字段、商品行实付/退款计数、售后单和操作日志。商城端与内管端共享 Mapper/Service，保留独立 Controller；小程序和 Vue 内管分别消费固定路径 GET/POST API。

**Tech Stack:** JDK 11、Spring Boot 2.7.18、Spring Security、MyBatis XML、MySQL 8、Druid、Flyway、Redis 6.2.13、JUnit 5/Mockito、微信小程序 TypeScript/Vant Weapp、Vue 3/TypeScript/Element Plus/Vitest。

**Spec:** `docs/superpowers/specs/2026-09-18-after-sales-design.md`

## Global Constraints

- 后端仅使用 GET、POST，不得出现路径变量；POST 参数使用 JSON Body。
- 所有写事务及只读事务显式声明 `@Transactional(..., rollbackFor = Exception.class)`。
- 不修改 Flyway V1-V11；新增迁移固定为 `V12__create_after_sale.sql`。
- 保留现有 `/api/orders`、`/api/orders/detail`、`/admin/api/orders` 和响应字段兼容性。
- 商城 Token 与管理员 Token 继续隔离，分别使用 8080 和 8081。
- 退款只做业务记账，不接第三方支付。
- 所有生产行为先写失败测试，确认失败原因正确后再写最小实现。
- 三个 Git 项目分别提交：`mallService`、`mallPage`、`mallManagePage`。

---

## 文件结构

### `mallService`

- `mall-database/.../V12__create_after_sale.sql`：订单履约字段、商品行金额/退款计数、售后单与日志。
- `mall-order/.../order/`：订单状态、金额分摊、履约服务和 DTO。
- `mall-order/.../aftersale/`：售后模型、DTO、Mapper、XML、商城服务、内管服务、查询服务。
- `mall-shop-app/.../order/controller/OrderController.java`：商城订单动作。
- `mall-shop-app/.../aftersale/controller/AfterSaleController.java`：商城售后接口。
- `mall-admin-app/.../order/AdminOrderController.java`：内管发货接口。
- `mall-admin-app/.../aftersale/AdminAfterSaleController.java`：内管售后接口。
- `mall-common/.../api/ErrorCode.java`：订单与售后业务错误。

### `mallPage`

- `miniprogram/services/order.ts`、`after-sale.ts`：订单动作和售后 API。
- `miniprogram/models/index.ts`、`config/api.ts`：类型和固定接口路径。
- `miniprogram/utils/order-actions.ts`：可测试的状态动作推导。
- `pages/orders`、`order-detail`、`after-sales`、`after-sale-apply`、`after-sale-detail`：商城交互页面。
- `pages/profile`、`app.json`：计数和入口。

### `mallManagePage`

- `src/types/order.ts`、`after-sale.ts`：订单与售后类型。
- `src/services/order-api.ts`、`after-sale-api.ts`：内管 API。
- `src/views/OrderView.vue`：状态筛选、履约详情和发货。
- `src/views/AfterSaleView.vue`：售后查询、审核和收货退款。
- `src/router/index.ts`、`src/layouts/AdminLayout.vue`：售后菜单与固定路由。

---

### Task 1: 金额分摊和退款计算内核

**Files:**
- Create: `mall-order/src/main/java/com/mall/order/service/OrderAmountAllocator.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/service/RefundAmountCalculator.java`
- Modify: `mall-order/src/main/java/com/mall/order/model/OrderItemEntity.java`
- Test: `mall-order/src/test/java/com/mall/order/service/OrderAmountAllocatorTest.java`
- Test: `mall-order/src/test/java/com/mall/aftersale/service/RefundAmountCalculatorTest.java`

**Interfaces:**
- Produces: `OrderAmountAllocator.allocate(List<OrderItemEntity>, BigDecimal)`，原地写入每行 `paymentAmount`。
- Produces: `RefundAmountCalculator.calculate(BigDecimal, int, int, BigDecimal, int): BigDecimal`。

- [ ] **Step 1: 写订单金额分摊失败测试**

```java
@Test
void givesRoundingRemainderToLastItem() {
    List<OrderItemEntity> items = Arrays.asList(item("1.00"), item("1.00"), item("1.00"));
    OrderAmountAllocator.allocate(items, new BigDecimal("10.00"));
    assertEquals(new BigDecimal("3.33"), items.get(0).getPaymentAmount());
    assertEquals(new BigDecimal("3.33"), items.get(1).getPaymentAmount());
    assertEquals(new BigDecimal("3.34"), items.get(2).getPaymentAmount());
}

private static OrderItemEntity item(String subtotal) {
    OrderItemEntity item = new OrderItemEntity();
    item.setSubtotal(new BigDecimal(subtotal));
    return item;
}
```

- [ ] **Step 2: 运行测试并确认因类不存在失败**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order -am -Dtest=OrderAmountAllocatorTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL，提示 `OrderAmountAllocator` 不存在。

- [ ] **Step 3: 实现金额分摊**

```java
public static void allocate(List<OrderItemEntity> items, BigDecimal paymentAmount) {
    BigDecimal total = items.stream().map(OrderItemEntity::getSubtotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal allocated = BigDecimal.ZERO;
    for (int index = 0; index < items.size(); index++) {
        BigDecimal value = index == items.size() - 1
                ? paymentAmount.subtract(allocated)
                : paymentAmount.multiply(items.get(index).getSubtotal())
                    .divide(total, 2, RoundingMode.HALF_UP);
        items.get(index).setPaymentAmount(value.setScale(2, RoundingMode.HALF_UP));
        allocated = allocated.add(value);
    }
}
```

- [ ] **Step 4: 写退款累计差额失败测试并实现**

```java
@Test
void cumulativeRoundingRefundsExactlyTheLineAmount() {
    BigDecimal first = RefundAmountCalculator.calculate(new BigDecimal("10.00"), 3, 0,
            BigDecimal.ZERO, 1);
    BigDecimal second = RefundAmountCalculator.calculate(new BigDecimal("10.00"), 3, 1,
            first, 1);
    BigDecimal last = RefundAmountCalculator.calculate(new BigDecimal("10.00"), 3, 2,
            first.add(second), 1);
    assertEquals(new BigDecimal("3.33"), first);
    assertEquals(new BigDecimal("3.34"), second);
    assertEquals(new BigDecimal("3.33"), last);
    assertEquals(new BigDecimal("10.00"), first.add(second).add(last));
}
```

实现必须验证数量为正、累计数量不超过购买数量，并在最后一件直接返回 `linePaymentAmount - refundedAmount`。

- [ ] **Step 5: 运行两个测试并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order -am -Dtest=OrderAmountAllocatorTest,RefundAmountCalculatorTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: PASS。

Commit: `git add -A && git commit -m "feat: add order refund amount calculation"`

---

### Task 2: V12 数据库结构与持久化模型

**Files:**
- Create: `mall-database/src/main/resources/db/migration/V12__create_after_sale.sql`
- Modify: `mall-database/pom.xml`
- Test: `mall-database/src/test/java/com/mall/database/MigrationV12ContractTest.java`
- Modify: `mall-order/src/main/java/com/mall/order/model/OrderEntity.java`
- Modify: `mall-order/src/main/java/com/mall/order/model/OrderItemEntity.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/model/AfterSaleEntity.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/model/AfterSaleLogEntity.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/mapper/AfterSaleMapper.java`
- Create: `mall-order/src/main/resources/mapper/aftersale/AfterSaleMapper.xml`
- Modify: `mall-order/src/main/java/com/mall/order/mapper/OrderMapper.java`
- Modify: `mall-order/src/main/resources/mapper/order/OrderMapper.xml`
- Modify: `mall-product/src/main/java/com/mall/product/mapper/ProductSkuMapper.java`
- Modify: `mall-product/src/main/resources/mapper/product/ProductSkuMapper.xml`
- Modify: `mall-promotion/src/main/java/com/mall/coupon/mapper/CouponMapper.java`
- Modify: `mall-promotion/src/main/resources/mapper/coupon/CouponMapper.xml`

**Interfaces:**
- Produces: `AfterSaleMapper.findByIdForUpdate(long)`、`findByIdAndUserId(...)`、`insert(...)`、`insertLog(...)`、`updateStatus(...)`、`sumReservedQuantity(...)`、管理分页和详情查询。
- Produces: `OrderMapper.findByIdAndUserIdForUpdate(...)`、`findItemForUpdate(...)`、`updateStatus(...)`、`markItemRefunded(...)`。
- Produces: `ProductSkuMapper.restoreStockAndDecreaseSales(...)`、`decreaseSales(...)`。
- Produces: `CouponMapper.releaseUsedByOrder(...)`。

- [ ] **Step 1: 写迁移契约失败测试**

```java
@Test
void v12DeclaresAfterSaleTablesAndActiveConstraint() throws Exception {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(
            "db/migration/V12__create_after_sale.sql")) {
        assertNotNull(input);
        String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(sql.contains("CREATE TABLE mall_after_sale"));
        assertTrue(sql.contains("CREATE TABLE mall_after_sale_log"));
        assertTrue(sql.contains("uk_after_sale_item_active"));
        assertTrue(sql.contains("payment_amount"));
        assertTrue(sql.contains("refunded_quantity"));
    }
}
```

在 `mall-database/pom.xml` 增加 `spring-boot-starter-test` 的 test 依赖。

- [ ] **Step 2: 运行并确认因 V12 资源不存在失败**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-database -Dtest=MigrationV12ContractTest test`

Expected: FAIL at `assertNotNull(input)`。

- [ ] **Step 3: 创建 V12**

迁移必须执行以下精确结构：

```sql
ALTER TABLE mall_order
  ADD COLUMN paid_at DATETIME(3) NULL,
  ADD COLUMN shipping_company VARCHAR(64) NULL,
  ADD COLUMN tracking_no VARCHAR(64) NULL,
  ADD COLUMN shipped_at DATETIME(3) NULL,
  ADD COLUMN completed_at DATETIME(3) NULL,
  ADD COLUMN cancelled_at DATETIME(3) NULL;

ALTER TABLE mall_order_item
  ADD COLUMN payment_amount DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00,
  ADD COLUMN refunded_quantity INT UNSIGNED NOT NULL DEFAULT 0,
  ADD COLUMN refunded_amount DECIMAL(12,2) UNSIGNED NOT NULL DEFAULT 0.00;
```

使用临时表和 MySQL 8 窗口函数按订单分摊历史金额，避免直接更新目标表时重复读取：

```sql
CREATE TEMPORARY TABLE tmp_order_item_payment AS
SELECT id,
       CASE WHEN reverse_rank = 1
            THEN order_payment - SUM(CASE WHEN reverse_rank > 1 THEN proportional_amount ELSE 0 END)
                 OVER (PARTITION BY order_id)
            ELSE proportional_amount END AS allocated_amount
FROM (
    SELECT oi.id, oi.order_id, o.payment_amount AS order_payment,
           ROW_NUMBER() OVER (PARTITION BY oi.order_id ORDER BY oi.id DESC) AS reverse_rank,
           ROUND(o.payment_amount * oi.subtotal / NULLIF(o.total_amount, 0), 2) AS proportional_amount
    FROM mall_order_item oi
    INNER JOIN mall_order o ON o.id = oi.order_id
) allocation_source;

UPDATE mall_order_item oi
INNER JOIN tmp_order_item_payment allocation ON allocation.id = oi.id
SET oi.payment_amount = allocation.allocated_amount;

DROP TEMPORARY TABLE tmp_order_item_payment;
```

随后按规格第 5.2、5.3 节的明确列名和长度创建两张表、外键、查询索引和 `(order_item_id, active_marker)` 唯一索引。

- [ ] **Step 4: 增加实体、Mapper 和原子 SQL**

订单状态常量固定为：

```java
public static final int STATUS_PENDING_PAYMENT = 1;
public static final int STATUS_PENDING_SHIPMENT = 2;
public static final int STATUS_PENDING_RECEIPT = 3;
public static final int STATUS_COMPLETED = 4;
public static final int STATUS_CANCELLED = 5;
```

SKU 原子更新必须带销量下限：

```xml
<update id="restoreStockAndDecreaseSales">
  UPDATE product_sku
  SET stock = stock + #{quantity}, sales = sales - #{quantity}, updated_at = CURRENT_TIMESTAMP(3)
  WHERE id = #{id} AND sales &gt;= #{quantity}
</update>
<update id="decreaseSales">
  UPDATE product_sku
  SET sales = sales - #{quantity}, updated_at = CURRENT_TIMESTAMP(3)
  WHERE id = #{id} AND sales &gt;= #{quantity}
</update>
```

退券 SQL 仅释放当前订单实际占用的券：

```xml
UPDATE user_coupon SET status = 1, used_at = NULL, used_order_id = NULL
WHERE used_order_id = #{orderId} AND user_id = #{userId} AND status = 2
```

- [ ] **Step 5: 运行迁移契约、编译并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-database,mall-order -am test`

Expected: PASS。

Commit: `git add -A && git commit -m "feat: add after-sales persistence model"`

---

### Task 3: 订单履约服务与接口

**Files:**
- Modify: `mall-order/src/test/java/com/mall/order/service/OrderServiceTest.java`
- Create: `mall-order/src/test/java/com/mall/order/service/AdminOrderServiceTest.java`
- Modify: `mall-order/src/main/java/com/mall/order/service/OrderService.java`
- Create: `mall-order/src/main/java/com/mall/order/service/AdminOrderService.java`
- Create: `mall-order/src/main/java/com/mall/order/dto/OrderActionRequest.java`
- Create: `mall-order/src/main/java/com/mall/order/dto/ShipOrderRequest.java`
- Modify: `mall-order/src/main/java/com/mall/order/dto/OrderItemResponse.java`
- Modify: `mall-order/src/main/java/com/mall/order/dto/OrderResponse.java`
- Modify: `mall-order/src/main/java/com/mall/order/dto/OrderSummaryResponse.java`
- Modify: `mall-order/src/main/java/com/mall/order/dto/AdminOrderResponse.java`
- Modify: `mall-order/src/main/java/com/mall/order/dto/AdminOrderDetailResponse.java`
- Create: `mall-common/src/main/java/com/mall/common/config/TimeConfig.java`
- Modify: `mall-shop-app/src/main/java/com/mall/order/controller/OrderController.java`
- Modify: `mall-admin-app/src/main/java/com/mall/admin/order/AdminOrderController.java`
- Modify: `mall-common/src/main/java/com/mall/common/api/ErrorCode.java`

**Interfaces:**
- Produces: `OrderService.pay(userId, orderId, baseUrl)`、`cancel(...)`、`confirmReceipt(...)`。
- Produces: `AdminOrderService.ship(orderId, shippingCompany, trackingNo, baseUrl)`。
- Produces: `Clock` Spring Bean，所有新状态时间通过该 Bean 获取，测试注入 `Clock.fixed(...)`。

- [ ] **Step 1: 写支付、取消、收货失败测试**

```java
@Test
void cancelPendingOrderRestoresStockSalesAndCoupon() {
    when(orderMapper.findByIdAndUserIdForUpdate(99L, 7L)).thenReturn(order(1));
    when(orderMapper.findItemsByOrderId(99L)).thenReturn(Collections.singletonList(item(111L, 2)));
    when(skuMapper.restoreStockAndDecreaseSales(111L, 2)).thenReturn(1);
    when(orderMapper.updateStatus(99L, 1, 5, any(LocalDateTime.class))).thenReturn(1);
    service.cancel(7L, 99L, "http://localhost");
    verify(couponMapper).releaseUsedByOrder(99L, 7L);
}
```

分别增加：支付只允许状态 1、确认收货只允许状态 3、重复动作返回 `ORDER_STATUS_INVALID`。

测试类中的固定构造器必须明确设置 `id`、`userId`、`status`、`skuId`、`quantity` 和金额；服务使用固定时钟：

```java
private static final Clock FIXED_CLOCK = Clock.fixed(
        Instant.parse("2026-09-18T02:00:00Z"), ZoneId.of("Asia/Shanghai"));
```

- [ ] **Step 2: 运行测试并确认缺少方法失败**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order -am -Dtest=OrderServiceTest,AdminOrderServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 3: 最小实现订单状态流转**

所有方法锁订单并使用带旧状态条件的更新：

```java
@Transactional(rollbackFor = Exception.class)
public OrderResponse pay(Long userId, Long orderId, String baseUrl) {
    OrderEntity order = requiredOwnedOrderForUpdate(userId, orderId);
    requireStatus(order, OrderEntity.STATUS_PENDING_PAYMENT);
    LocalDateTime now = LocalDateTime.now(clock);
    requireUpdated(orderMapper.markPaid(orderId, OrderEntity.STATUS_PENDING_PAYMENT, now));
    return detail(userId, orderId, baseUrl);
}
```

时钟配置为：

```java
@Bean
public Clock applicationClock() {
    return Clock.systemDefaultZone();
}
```

取消时逐行执行 `restoreStockAndDecreaseSales`，发货时要求存在未退款待履约数量，确认收货写入 `completed_at`。

- [ ] **Step 4: 接入 Controller 和响应 DTO**

商城 Controller 增加三个固定 POST 路径；内管 Controller 增加 `/ship`。请求 DTO 的 `orderId` 使用 `@NotNull @Positive`，物流字段使用 `@NotBlank @Size(max=64)`。

- [ ] **Step 5: 运行订单测试并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order,mall-shop-app,mall-admin-app -am test`

Commit: `git add -A && git commit -m "feat: add order fulfillment workflow"`

---

### Task 4: 商城用户售后申请、查询与退货

**Files:**
- Create: `mall-order/src/test/java/com/mall/aftersale/service/AfterSaleServiceTest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/service/AfterSaleService.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/ApplyAfterSaleRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AfterSaleActionRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/ReturnShipmentRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AfterSaleResponse.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AfterSaleDetailResponse.java`
- Create: `mall-shop-app/src/main/java/com/mall/aftersale/controller/AfterSaleController.java`

**Interfaces:**
- Produces: `apply`、`listMine`、`detailMine`、`cancel`、`submitReturnShipment`。

- [ ] **Step 1: 写申请资格和金额失败测试**

```java
@Test
void appliesPartialRefundUsingCumulativePaidAmount() {
    when(orderMapper.findItemForUpdate(21L, 7L)).thenReturn(item(21L, 18L, 8L, 3,
            new BigDecimal("10.00"), 1, new BigDecimal("3.33")));
    when(orderMapper.findByIdAndUserIdForUpdate(18L, 7L)).thenReturn(order(3));
    when(afterSaleMapper.sumReservedQuantity(21L)).thenReturn(0);
    when(afterSaleMapper.insert(any())).thenAnswer(invocation -> {
        AfterSaleEntity value = invocation.getArgument(0); value.setId(30L); return 1;
    });
    AfterSaleResponse response = service.apply(7L, request(21L, 1, 1));
    assertEquals(new BigDecimal("3.34"), response.getRefundAmount());
}
```

继续增加测试：待发货拒绝退货退款、完成 7 天后拒绝、进行中记录拒绝、数量超限、他人订单 404、说明为空 400 由 MVC 覆盖。

测试类声明同样的本地 `FIXED_CLOCK` 常量，并让已完成订单分别设置为 `now.minusDays(7)` 与 `now.minusDays(7).minusNanos(1)`，明确验证边界包含第 7 天、排除超过第 7 天的时刻。

- [ ] **Step 2: 运行并确认 `AfterSaleService` 不存在**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order -am -Dtest=AfterSaleServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 3: 实现申请和用户状态操作**

申请事务按顺序锁订单商品行、订单，校验状态和 7 天期限，计算剩余数量/金额，生成 `ASyyyyMMddHHmmssSSSxxxxxxxx` 售后号，插入售后单和申请日志。捕获 `(order_item_id, active_marker)` 唯一冲突并转为 `AFTER_SALE_ACTIVE_EXISTS`。

撤销只允许状态 1、2；提交物流只允许状态 2。进入终态时将 `active_marker` 更新为 `NULL`。

- [ ] **Step 4: 增加商城 Controller**

```java
@PostMapping("/apply")
public ResponseEntity<ApiResponse<AfterSaleResponse>> apply(
        @AuthenticationPrincipal AuthenticatedUser user,
        @Valid @RequestBody ApplyAfterSaleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(service.apply(user.getUserId(), request)));
}
```

其余路径严格使用规格第 8.3 节固定地址。

- [ ] **Step 5: 运行测试并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order,mall-shop-app -am test`

Commit: `git add -A && git commit -m "feat: add customer after-sales workflow"`

---

### Task 5: 内管售后审核、退款与库存销量副作用

**Files:**
- Create: `mall-order/src/test/java/com/mall/aftersale/service/AdminAfterSaleServiceTest.java`
- Create: `mall-order/src/test/java/com/mall/aftersale/service/AdminAfterSaleQueryServiceTest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/service/AdminAfterSaleService.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/service/AdminAfterSaleQueryService.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AdminApproveAfterSaleRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AdminRejectAfterSaleRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AdminConfirmReturnRequest.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AdminAfterSaleResponse.java`
- Create: `mall-order/src/main/java/com/mall/aftersale/dto/AdminAfterSaleDetailResponse.java`
- Create: `mall-admin-app/src/main/java/com/mall/admin/aftersale/AdminAfterSaleController.java`

**Interfaces:**
- Produces: `approve(adminId, request)`、`reject(...)`、`confirmReceipt(...)`。
- Produces: `page(...)`、`detail(afterSaleId, baseUrl)`。

- [ ] **Step 1: 写两类审核失败测试**

```java
@Test
void approvingPreShipmentRefundRestoresStockAndDecreasesSalesOnce() {
    when(mapper.findByIdForUpdate(30L)).thenReturn(afterSale(1, 1, 2));
    when(orderMapper.findByIdForUpdate(18L)).thenReturn(order(2));
    when(skuMapper.restoreStockAndDecreaseSales(8L, 2)).thenReturn(1);
    service.approve(1L, approve(30L, null));
    verify(orderMapper).markItemRefunded(21L, 2, new BigDecimal("6.67"));
    verify(mapper).insertLog(argThat(log -> log.getToStatus() == 4));
}
```

增加测试：发货后仅退款只减销量；退货退款同意进入状态 2 且要求地址；拒绝要求原因并释放 active marker；状态 3 确认收货恢复库存、减销量并完成退款；重复确认返回 `AFTER_SALE_STATUS_INVALID`。

- [ ] **Step 2: 运行并确认服务类不存在**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order -am -Dtest=AdminAfterSaleServiceTest,AdminAfterSaleQueryServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

- [ ] **Step 3: 实现审核事务**

仅退款同意时，根据订单状态选择：

```java
if (order.getStatus() == OrderEntity.STATUS_PENDING_SHIPMENT) {
    requireUpdated(skuMapper.restoreStockAndDecreaseSales(sale.getSkuId(), sale.getQuantity()));
} else {
    requireUpdated(skuMapper.decreaseSales(sale.getSkuId(), sale.getQuantity()));
}
completeRefund(sale, adminId, now);
```

退货退款同意只写状态 2 和退货地址；确认退货时执行恢复库存并减销量、更新商品行退款计数、售后终态和日志。所有更新必须检查影响行数。

- [ ] **Step 4: 实现分页查询和 Controller**

管理分页参数：`afterSaleNo`、`orderNo`、`userId`、`type`、`status`、`page=1`、`size=20`，最大 100。详情返回售后、订单、商品行和日志。

- [ ] **Step 5: 运行测试并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-order,mall-admin-app -am test`

Commit: `git add -A && git commit -m "feat: add admin after-sales review"`

---

### Task 6: 后端 HTTP、安全与路由契约

**Files:**
- Modify: `mall-shop-app/src/test/java/com/mall/security/SecurityAndMethodPolicyMvcTest.java`
- Modify: `mall-shop-app/src/test/java/com/mall/architecture/ControllerRoutePolicyTest.java`
- Modify: `mall-admin-app/src/test/java/com/mall/admin/security/AdminSecurityMvcTest.java`
- Modify: `mall-admin-app/src/test/java/com/mall/admin/architecture/AdminControllerRoutePolicyTest.java`
- Modify: `mall-shop-app/src/main/java/com/mall/security/SecurityConfig.java`
- Modify: `mall-admin-app/src/main/java/com/mall/admin/config/AdminSecurityConfig.java`

**Interfaces:**
- Consumes: Tasks 3-5 的 Controller 和 Service。
- Produces: 统一认证、校验和 405 契约。

- [ ] **Step 1: 写商城 MVC 失败测试**

```java
@Test
void authenticatedUserCanApplyForAfterSale() throws Exception {
    authenticateUser(9L);
    when(afterSaleService.apply(eq(9L), any())).thenReturn(afterSaleResponse());
    mockMvc.perform(post("/api/after-sales/apply")
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderItemId\":21,\"type\":1,\"quantity\":1," +
                    "\"reasonCode\":\"DAMAGED\",\"description\":\"水果到货破损\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(30));
}
```

增加无 Token 401、空说明 400、非 GET/POST 405。

- [ ] **Step 2: 写内管 MVC 失败测试**

覆盖分页查询、同意退货退款缺少地址 400、拒绝缺少原因 400、确认收货成功和无 Token 401。

- [ ] **Step 3: 运行并确认新 Controller 尚未纳入切片测试**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -pl mall-shop-app,mall-admin-app -am test`

- [ ] **Step 4: 将 Controller/MockBean 纳入测试并修复契约**

确保两个路由架构测试自动扫描新 Controller，并验证所有映射只有 `RequestMethod.GET/POST`、路径不含 `{`。安全配置不新增公开售后路径。

- [ ] **Step 5: 运行后端全量测试并提交**

Run: `mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml test`

Expected: reactor 全部模块 PASS。

Commit: `git add -A && git commit -m "test: cover after-sales http contracts"`

---

### Task 7: 小程序订单/售后数据层和动作规则

**Files (`mallPage`):**
- Modify: `package.json`
- Create: `miniprogram/services/order.spec.ts`
- Create: `miniprogram/services/after-sale.spec.ts`
- Create: `miniprogram/utils/order-actions.spec.ts`
- Create: `miniprogram/utils/order-actions.ts`
- Modify: `miniprogram/config/api.ts`
- Modify: `miniprogram/models/index.ts`
- Modify: `miniprogram/services/order.ts`
- Create: `miniprogram/services/after-sale.ts`

**Interfaces:**
- Produces: `payOrder`、`cancelOrder`、`confirmOrderReceipt`、`applyAfterSale`、`getAfterSales`、`getAfterSaleDetail`、`cancelAfterSale`、`submitReturnShipment`。
- Produces: `getOrderActions(status, refundable)` 和 `getAfterSaleActions(status)`。

- [ ] **Step 1: 加入 Vitest 并写失败测试**

在 `package.json` 增加 `"test": "vitest run"`，devDependency 增加与 Node 版本兼容的 `vitest`。测试 mock `./request`：

```ts
it('posts return shipment as JSON', async () => {
  vi.mocked(request).mockResolvedValue(afterSaleDto)
  await submitReturnShipment('30', '顺丰速运', 'SF123456')
  expect(request).toHaveBeenCalledWith({
    url: API.afterSaleReturnShipment,
    method: 'POST',
    data: { afterSaleId: '30', shippingCompany: '顺丰速运', trackingNo: 'SF123456' },
  })
})
```

- [ ] **Step 2: 运行并确认因导出不存在失败**

Run: `npm test -- --run`（目录 `mallPage`）

- [ ] **Step 3: 实现类型、固定路径和 API 映射**

金额 DTO 统一从元转换为分；ID 统一映射为字符串。POST 显式传 `method: 'POST'`，GET 通过 `data` 生成查询参数，保持现有请求封装约定。

- [ ] **Step 4: 实现可测试动作矩阵**

```ts
export function getOrderActions(status: number, refundable: boolean): OrderAction[] {
  if (status === 1) return ['pay', 'cancel']
  if (status === 3) return refundable ? ['confirmReceipt', 'afterSale'] : ['confirmReceipt']
  if ((status === 2 || status === 4) && refundable) return ['afterSale']
  return []
}
```

售后状态 1、2 可撤销，状态 2 可提交退货物流，其他状态无用户动作。

- [ ] **Step 5: 运行测试、检查并提交**

Run: `npm test && npm run check`

Commit: `git add -A && git commit -m "feat: add mini program after-sales data layer"`

---

### Task 8: 小程序订单履约与售后页面

**Files (`mallPage`):**
- Modify: `miniprogram/app.json`
- Modify: `miniprogram/pages/profile/index.ts`
- Modify: `miniprogram/pages/profile/index.wxml`
- Modify: `miniprogram/pages/orders/index.ts`
- Modify: `miniprogram/pages/orders/index.wxml`
- Modify: `miniprogram/pages/orders/index.wxss`
- Create: `miniprogram/pages/order-detail/index.{ts,json,wxml,wxss}`
- Create: `miniprogram/pages/after-sales/index.{ts,json,wxml,wxss}`
- Create: `miniprogram/pages/after-sale-apply/index.{ts,json,wxml,wxss}`
- Create: `miniprogram/pages/after-sale-detail/index.{ts,json,wxml,wxss}`

**Interfaces:**
- Consumes: Task 7 服务和动作矩阵。

- [ ] **Step 1: 使用 `impeccable:impeccable` 审核现有小程序视觉模式**

读取该技能后，只复用现有绿色品牌、卡片圆角、Vant 组件和间距变量；不引入新的图标包或视觉主题。

- [ ] **Step 2: 先让类型检查暴露缺失页面数据**

在新页面 TS 中声明完整 `data` 类型并引用 Task 7 导出，然后运行 `npm run typecheck`。

Expected: FAIL，原因是页面文件/导出尚未补齐，而不是语法错误。

- [ ] **Step 3: 实现订单入口和详情**

订单卡片点击进入固定路径 `/pages/order-detail/index?orderId=...`。订单详情依据动作矩阵显示按钮；所有写操作具有 `submitting` 锁、二次确认和成功后重载。

- [ ] **Step 4: 实现售后申请、列表和详情**

申请页从订单详情接收 `orderId`、`orderItemId`，重新请求订单详情确认可申请数量，不信任 URL 金额。预计退款金额使用后端详情中的商品行实付/退款数据展示，提交结果以服务端返回为准。详情页在状态 2 显示物流表单，在状态 1/2 显示撤销。

- [ ] **Step 5: 运行检查并提交**

Run: `npm test && npm run check`

Commit: `git add -A && git commit -m "feat: add mini program after-sales pages"`

---

### Task 9: 内管订单/售后 API 与类型

**Files (`mallManagePage`):**
- Modify: `src/types/order.ts`
- Create: `src/types/after-sale.ts`
- Modify: `src/services/order-api.spec.ts`
- Modify: `src/services/order-api.ts`
- Create: `src/services/after-sale-api.spec.ts`
- Create: `src/services/after-sale-api.ts`

**Interfaces:**
- Produces: `shipOrder`、`listAfterSales`、`getAfterSaleDetail`、`approveAfterSale`、`rejectAfterSale`、`confirmAfterSaleReceipt`。

- [ ] **Step 1: 写发货 API 失败测试**

```ts
it('ships an order with a POST body', async () => {
  mock.onPost('/orders/ship').reply(200, ok(detail))
  await shipOrder({ orderId: 18, shippingCompany: '顺丰速运', trackingNo: 'SF123' })
  expect(JSON.parse(mock.history.post[0]!.data)).toEqual({
    orderId: 18, shippingCompany: '顺丰速运', trackingNo: 'SF123',
  })
})
```

- [ ] **Step 2: 写售后 API 失败测试**

验证分页过滤器会 trim 售后单号/订单号，`size`、`type`、`status` 正确传递；同意、拒绝、确认收货均为 POST JSON。

- [ ] **Step 3: 运行并确认导出不存在**

Run: `npm test -- src/services/order-api.spec.ts src/services/after-sale-api.spec.ts`

- [ ] **Step 4: 实现类型与 API**

状态和类型使用 `as const` 数字常量；响应字段与后端 DTO 一致，不在组件中使用 `any`。

- [ ] **Step 5: 运行测试并提交**

Run: `npm test -- src/services/order-api.spec.ts src/services/after-sale-api.spec.ts && npm run typecheck`

Commit: `git add -A && git commit -m "feat: add admin after-sales api"`

---

### Task 10: 内管订单发货与售后管理页面

**Files (`mallManagePage`):**
- Modify: `src/views/OrderView.spec.ts`
- Modify: `src/views/OrderView.vue`
- Create: `src/views/AfterSaleView.spec.ts`
- Create: `src/views/AfterSaleView.vue`
- Modify: `src/router/index.spec.ts`
- Modify: `src/router/index.ts`
- Modify: `src/layouts/AdminLayout.vue`
- Modify: `src/styles/main.css`

**Interfaces:**
- Consumes: Task 9 API。

- [ ] **Step 1: 使用 `impeccable:impeccable` 明确页面层级与交互状态**

保持当前侧边栏、数据卡片、筛选栏、表格和抽屉模式；审核操作使用对话框，危险/不可逆操作提供清晰确认文案，不新增装饰性渐变或不一致色彩。

- [ ] **Step 2: 写订单发货和售后页面失败测试**

```ts
it('requires return address before approving a return refund', async () => {
  vi.mocked(api.getAfterSaleDetail).mockResolvedValue(returnRefundDetail)
  const wrapper = mountView()
  await openFirstDetail(wrapper)
  await buttonByText(wrapper, '同意').trigger('click')
  await buttonByText(wrapper, '确认同意').trigger('click')
  expect(api.approveAfterSale).not.toHaveBeenCalled()
  expect(message.warning).toHaveBeenCalledWith('请填写退货地址或退货说明')
})
```

另外覆盖：订单发货表单、组合筛选、拒绝原因、确认收货二次确认、请求失败重试、路由 `/after-sales`。

- [ ] **Step 3: 运行并确认页面/操作不存在**

Run: `npm test -- src/views/OrderView.spec.ts src/views/AfterSaleView.spec.ts src/router/index.spec.ts`

- [ ] **Step 4: 实现页面与响应式样式**

售后表格展示售后号、订单、用户、商品、类型、金额、状态、时间；详情抽屉按时间顺序展示日志。按钮使用状态守卫和 `submitting`，成功后关闭对话框并同时刷新列表、详情。窄屏下筛选器单列、抽屉不超过 `94vw`。

- [ ] **Step 5: 全量验证并提交**

Run: `npm test && npm run typecheck && npm run lint && npm run build`

Expected: 全部退出码 0。

Commit: `git add -A && git commit -m "feat: add admin after-sales management"`

---

### Task 11: 文档、完整构建与真实环境验收

**Files:**
- Modify: `README.md`
- Modify: `../mallPage/README.md`
- Modify: `../mallManagePage/README.md`

**Interfaces:**
- Documents: 订单状态、售后状态、接口、启动方式、账号和验收流程。

- [ ] **Step 1: 更新三个 README**

后端 README 列出规格第 8 节全部接口、状态值和环境变量；两个前端 README 列出新入口、页面和代理端口。明确退款为模拟业务记账，不对接支付网关。

- [ ] **Step 2: 后端全量测试和打包**

Run:

```bash
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml clean test
mvn -s /Library/Maven/apache-maven-3.6.3/conf/settings.xml -DskipTests package
```

Expected: 两次 reactor 均 `BUILD SUCCESS`，无失败测试。

- [ ] **Step 3: 两个前端完整验证**

Run in `mallPage`: `npm test && npm run check`

Run in `mallManagePage`: `npm test && npm run typecheck && npm run lint && npm run build`

Expected: 所有命令退出码 0。

- [ ] **Step 4: 启动依赖和两个服务**

使用 `DB_PASSWORD=123456`、`REDIS_PASSWORD=123456`、有效 Base64 JWT 密钥启动商城端 8080 和内管端 8081。日志必须出现 Flyway schema version `12` 和两端 `Started ...Application`。

- [ ] **Step 5: 执行真实 API 验收**

使用独立验收用户走通：注册/登录 → 加购 → 下单 → 模拟支付 → 内管发货 → 确认收货 → 申请退货退款 → 内管同意 → 用户提交物流 → 内管确认收货。验证：

```text
售后终态 = 已退款
订单商品 refunded_quantity = 申请数量
订单商品 refunded_amount = 售后退款金额
SKU stock 增加申请数量
SKU sales 减少申请数量
售后日志包含完整 5 个阶段
```

再创建待发货仅退款，验证库存增加、销量减少；对 PUT、DELETE、PATCH、HEAD、OPTIONS 验证 HTTP 405 和 `Allow: GET, POST`。

- [ ] **Step 6: 检查三个仓库状态并提交文档**

Run: `git status --short` in each repository。

Expected: 仅存在预期文件；测试产物未被跟踪。

Commits:

```bash
git add -A && git commit -m "docs: document after-sales workflow"
```

分别在三个仓库执行提交。

---

## 最终验收清单

- [ ] V12 在真实 MySQL 8 上执行成功，V1-V11 校验值未变化。
- [ ] 两种售后类型和全部状态流转符合设计。
- [ ] 优惠分摊和部分退款累计值精确到分且不超额。
- [ ] 取消/退款对库存与净销量的影响准确且幂等。
- [ ] 商城端、内管端 API 仅 GET/POST 且无路径变量。
- [ ] 小程序可完成用户侧全流程。
- [ ] 内管可完成发货、审核和收货退款全流程。
- [ ] Maven 测试/打包、两个前端测试/检查/构建均通过。
- [ ] 8080、8081 服务运行，Swagger 可访问。

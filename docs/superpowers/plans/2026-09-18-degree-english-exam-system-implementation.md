# 成人学士学位英语模拟试卷系统 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 mallService 技术栈上交付包含五套原创题量、在线计时答题、自动保存、客观题判分、错题解析和主观题自评的电脑 Web 模拟考试系统。

**Architecture:** 在 mallService 中新增独立 `mall-exam` 领域模块，由商城端和管理端应用分别暴露考生 API 与管理员 API；MySQL 保存题库和不可变试卷快照，Redis 只承担并发组卷短锁。新建 Vue 3 项目 `examPage` 作为电脑考试端，并扩展现有 `mallManagePage` 管理题源、题库、导入、蓝图和考试记录。

**Tech Stack:** JDK 11、Spring Boot 2.7.18、Maven、MyBatis、MySQL 8、Flyway、Redis、JUnit 5、Mockito、Vue 3.5、TypeScript 5.9、Vite 6、Pinia 3、Vue Router 4、Axios 1、Vitest 3、Playwright。

**Spec:** `docs/superpowers/specs/2026-09-18-degree-english-exam-system-design.md`

## Global Constraints

- 目标大纲固定为《成人高等教育本科生学士学位英语水平考试大纲（非英语专业）》（2016 年 12 月第二版）。
- 每套试卷固定 52 个答题项、100 分、120 分钟；客观题 70 分，翻译和写作各 15 分且仅由考生自评。
- 一名用户同一时间只能有一场未交卷考试；刷新或重新登录必须恢复相同试卷和截止时间。
- 对话与阅读必须以完整题组抽取；同一试卷内不得重复题组或单题。
- 交卷前任何考生端 DTO 都不能声明正确答案、解析、参考译文、范文或评分量表字段。
- 首版只允许 `ORIGINAL`、`AUTHORIZED`、`PUBLIC_OFFICIAL` 且审核通过的来源参与组卷；不实现主动抓取 URL。
- 所有业务接口只使用 GET/POST，不使用路径变量；继续使用 `ApiResponse`、`PageResponse` 和现有 JWT 体系。
- 现有水果商城 `/api/auth`、商品、购物车、订单、地址和优惠券接口契约不得改变。
- 考生端最低支持 1024px 宽度，并验证 1280px、1440px、1920px。
- 新功能和缺陷修复必须遵循 RED → GREEN → REFACTOR；每个任务提交前运行该任务列出的完整验证命令。

---

## File Structure

### `/Users/chengchengzi/Documents/ChatGPT/mallService`

- `mall-exam/`：考试领域模型、DTO、Mapper、Service、组卷器、导入校验器。
- `mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql`：考试领域表结构。
- `mall-database/src/main/java/db/migration/V13__seed_degree_english_exam.java`：读取五套 JSON 并批量写入。
- `mall-database/src/main/resources/db/seed/degree-english-v1/`：五套原创题库 JSON。
- `mall-shop-app/src/main/java/com/mall/exam/controller/ExamController.java`：考生 API。
- `mall-admin-app/src/main/java/com/mall/admin/exam/`：题源、题库、导入、蓝图和记录 API。

### `/Users/chengchengzi/Documents/ChatGPT/examPage`

- `src/services/`：认证、考试 API 与统一 HTTP 客户端。
- `src/stores/`：认证会话、当前考试、离线保存队列。
- `src/views/`：登录、首页、答题、结果、历史。
- `src/components/exam/`：倒计时、章节导航、题面、答题卡、保存状态。

### `/Users/chengchengzi/Documents/ChatGPT/mallManagePage`

- `src/types/exam.ts`、`src/services/exam-api.ts`：考试管理契约。
- `src/views/exam/`：题库、题源、导入、蓝图和考试记录页面。
- `src/router/index.ts`、`src/layouts/AdminLayout.vue`：路由与菜单。

---

### Task 1: 建立 mall-exam 模块、考试蓝图和数据库结构

**Files:**
- Modify: `pom.xml`
- Modify: `mall-shop-app/pom.xml`
- Modify: `mall-admin-app/pom.xml`
- Modify: `mall-shop-app/src/main/resources/application.yml`
- Modify: `mall-admin-app/src/main/resources/application.yml`
- Modify: `mall-admin-app/src/main/java/com/mall/admin/config/AdminMyBatisConfig.java`
- Modify: `mall-common/src/main/java/com/mall/common/api/ErrorCode.java`
- Create: `mall-exam/pom.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/blueprint/ExamBlueprintDefinition.java`
- Create: `mall-exam/src/main/java/com/mall/exam/blueprint/ExamSectionSpec.java`
- Create: `mall-exam/src/test/java/com/mall/exam/blueprint/ExamBlueprintDefinitionTest.java`
- Create: `mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql`

**Interfaces:**
- Produces: `ExamBlueprintDefinition.degreeEnglish2016V2()`，返回时长 120、总分 100 和六个蓝图部分。
- Produces: V12 中的 `exam_question_source`、`exam_import_batch`、`exam_question_group`、`exam_question`、`exam_blueprint`、`exam_blueprint_section`、`exam_paper`、`exam_paper_group_snapshot`、`exam_paper_item_snapshot`、`exam_answer`、`exam_wrong_record`。

- [ ] **Step 1: 写蓝图失败测试**

```java
@Test
void degreeEnglishBlueprintHasApprovedStructure() {
    ExamBlueprintDefinition blueprint = ExamBlueprintDefinition.degreeEnglish2016V2();
    assertEquals("DEGREE_ENGLISH_2016_V2", blueprint.getCode());
    assertEquals(120, blueprint.getDurationMinutes());
    assertEquals(100, blueprint.getTotalScore());
    assertEquals(52, blueprint.getSections().stream().mapToInt(ExamSectionSpec::getAnswerItemCount).sum());
    assertEquals(70, blueprint.getSections().stream().filter(s -> !s.isSubjective())
            .mapToInt(ExamSectionSpec::getSectionScore).sum());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-exam -am -Dtest=ExamBlueprintDefinitionTest -Dsurefire.failIfNoSpecifiedTests=false test`

Expected: FAIL，因为 `mall-exam` 模块和蓝图类尚不存在。

- [ ] **Step 3: 创建模块和最小蓝图实现**

在父 POM 注册 `mall-exam`，在两个应用 POM 增加该模块依赖；两个应用的 `type-aliases-package` 增加考试模型包，`AdminMyBatisConfig` 的 `@MapperScan` 增加 `com.mall.exam` Mapper 包。`ExamBlueprintDefinition.degreeEnglish2016V2()` 必须返回以下精确规格：

```java
Arrays.asList(
    new ExamSectionSpec("DIALOGUE", 1, 3, 10, 10, false),
    new ExamSectionSpec("READING", 2, 4, 20, 40, false),
    new ExamSectionSpec("VOCABULARY", 3, 0, 10, 10, false),
    new ExamSectionSpec("GRAMMAR", 4, 0, 10, 10, false),
    new ExamSectionSpec("TRANSLATION", 5, 0, 1, 15, true),
    new ExamSectionSpec("WRITING", 6, 0, 1, 15, true)
)
```

V12 使用 `utf8mb4_0900_ai_ci`、毫秒级时间、外键和唯一键；`exam_answer` 对 `(paper_id, paper_item_id)` 唯一，试卷状态只允许 `IN_PROGRESS`、`SUBMITTED`、`AUTO_SUBMITTED`，JSON 内容使用 MySQL `JSON` 字段。

- [ ] **Step 4: 增加考试错误码**

在 `ErrorCode` 增加设计文档中的九个 `EXAM_*` 错误码，中文消息与规格一致。

- [ ] **Step 5: 运行模块与全量编译验证 GREEN**

Run: `mvn -pl mall-exam -am test`

Expected: PASS，蓝图断言全部通过。

- [ ] **Step 6: 提交**

```bash
git add pom.xml mall-exam mall-shop-app/pom.xml mall-admin-app/pom.xml mall-shop-app/src/main/resources/application.yml mall-admin-app/src/main/resources/application.yml mall-admin-app/src/main/java/com/mall/admin/config/AdminMyBatisConfig.java mall-common/src/main/java/com/mall/common/api/ErrorCode.java mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql
git commit -m "feat: establish exam domain schema and blueprint"
```

---

### Task 2: 实现题源、题组和题目的治理规则

**Files:**
- Create: `mall-exam/src/main/java/com/mall/exam/source/model/QuestionSourceEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/source/mapper/QuestionSourceMapper.java`
- Create: `mall-exam/src/main/resources/mapper/exam/QuestionSourceMapper.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/question/model/QuestionGroupEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/question/model/QuestionEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/question/mapper/QuestionMapper.java`
- Create: `mall-exam/src/main/resources/mapper/exam/QuestionMapper.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/question/service/QuestionGovernanceService.java`
- Create: `mall-exam/src/test/java/com/mall/exam/question/service/QuestionGovernanceServiceTest.java`

**Interfaces:**
- Produces: `QuestionGovernanceService.reviewSource(long sourceId, long adminId, SourceReviewCommand command)`。
- Produces: `QuestionGovernanceService.reviewQuestion(long questionId, long adminId, QuestionReviewCommand command)`。
- Produces: `QuestionGovernanceService.changeGroupStatus(long groupId, int status)`，对话/阅读整组更新。
- Consumes: Task 1 的考试错误码和 V12 表。

- [ ] **Step 1: 写未授权来源不能发布的失败测试**

```java
@Test
void cannotApproveQuestionWhoseSourceIsUnverified() {
    when(sourceMapper.findById(9L)).thenReturn(source(9L, "UNVERIFIED", "PENDING"));
    when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L, "PENDING"));

    BusinessException error = assertThrows(BusinessException.class,
            () -> service.reviewQuestion(21L, 3L, approveCommand()));

    assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
    verify(questionMapper, never()).updateReview(anyLong(), anyString(), anyLong(), any(), anyString());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-exam -Dtest=QuestionGovernanceServiceTest test`

Expected: FAIL，因为治理 Service 和 Mapper 尚不存在。

- [ ] **Step 3: 实现实体、Mapper 与治理 Service**

发布规则必须集中在 Service：只有版权状态为 `ORIGINAL`、`AUTHORIZED`、`PUBLIC_OFFICIAL` 且题源审核状态为 `APPROVED` 时，题目才能审核通过；对话和阅读的审核、启停通过 `group_id` 一次更新全部成员；被 `exam_paper_item_snapshot.source_question_id` 引用的题目不提供物理删除方法。

```java
private static boolean reusable(QuestionSourceEntity source) {
    return "APPROVED".equals(source.getReviewStatus())
            && Arrays.asList("ORIGINAL", "AUTHORIZED", "PUBLIC_OFFICIAL")
                     .contains(source.getCopyrightStatus());
}
```

- [ ] **Step 4: 增加整组发布、停用和引用保护测试**

测试必须断言 `DIALOGUE`/`READING` 调用批量更新、单题调用单题更新、存在快照引用时停用允许但删除被拒绝。

- [ ] **Step 5: 运行模块测试**

Run: `mvn -pl mall-exam test`

Expected: PASS，且 Mockito 无未使用 stub 警告。

- [ ] **Step 6: 提交**

```bash
git add mall-exam/src/main mall-exam/src/test
git commit -m "feat: govern exam sources and questions"
```

---

### Task 3: 实现 JSON/CSV 导入预览、指纹去重和批次提交

**Files:**
- Modify: `pom.xml`
- Modify: `mall-exam/pom.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/importer/QuestionFingerprint.java`
- Create: `mall-exam/src/main/java/com/mall/exam/importer/ExamImportParser.java`
- Create: `mall-exam/src/main/java/com/mall/exam/importer/ExamImportService.java`
- Create: `mall-exam/src/main/java/com/mall/exam/importer/dto/ImportPreviewResponse.java`
- Create: `mall-exam/src/main/java/com/mall/exam/importer/dto/ImportCommitRequest.java`
- Create: `mall-exam/src/test/java/com/mall/exam/importer/QuestionFingerprintTest.java`
- Create: `mall-exam/src/test/java/com/mall/exam/importer/ExamImportServiceTest.java`

**Interfaces:**
- Produces: `QuestionFingerprint.sha256(String groupMaterial, String stem)`。
- Produces: `ExamImportService.preview(long sourceId, String filename, byte[] bytes)`。
- Produces: `ExamImportService.commit(long batchId, long adminId)`。
- Consumes: Task 2 的题源与题库 Mapper。

- [ ] **Step 1: 写规范化指纹失败测试**

```java
@Test
void fingerprintIgnoresCaseAndRepeatedWhitespace() {
    String first = QuestionFingerprint.sha256("A  short\npassage", "What IS true?");
    String second = QuestionFingerprint.sha256(" a short passage ", "what is true?");
    assertEquals(first, second);
    assertEquals(64, first.length());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-exam -Dtest=QuestionFingerprintTest test`

Expected: FAIL，因为 `QuestionFingerprint` 不存在。

- [ ] **Step 3: 实现解析和预览校验**

父 POM锁定 `org.apache.commons:commons-csv:1.10.0`，`mall-exam` 显式依赖 Jackson Databind 和 Commons CSV。解析器只接受 `.json`、`.csv`，最大 5 MiB、最多 2,000 条记录；预览检查题型、题组结构、A–D 选项、客观题答案、解析、主观题参考材料和文本长度。

预览返回精确结构：

```java
new ImportPreviewResponse(batchId, totalRows, validRows, duplicateRows,
        Collections.unmodifiableList(errors), Collections.unmodifiableList(items));
```

- [ ] **Step 4: 写并实现提交保护测试**

测试 `commit` 在批次含结构错误时抛出 `EXAM_IMPORT_INVALID`，在来源未审核时抛出 `EXAM_SOURCE_UNAPPROVED`，成功时只写入 `DRAFT` 题目而不直接发布。

- [ ] **Step 5: 运行测试**

Run: `mvn -pl mall-exam test`

Expected: PASS，JSON 和含引号/换行的 CSV 样例均正确解析。

- [ ] **Step 6: 提交**

```bash
git add pom.xml mall-exam
git commit -m "feat: validate and import exam question batches"
```

---

### Task 4: 暴露考试管理 API

**Files:**
- Create: `mall-exam/src/main/java/com/mall/exam/admin/service/AdminExamQueryService.java`
- Create: `mall-exam/src/main/java/com/mall/exam/admin/dto/` 下来源、题库、蓝图、记录请求与响应类
- Create: `mall-admin-app/src/main/java/com/mall/admin/exam/AdminExamSourceController.java`
- Create: `mall-admin-app/src/main/java/com/mall/admin/exam/AdminExamQuestionController.java`
- Create: `mall-admin-app/src/main/java/com/mall/admin/exam/AdminExamImportController.java`
- Create: `mall-admin-app/src/main/java/com/mall/admin/exam/AdminExamBlueprintController.java`
- Create: `mall-admin-app/src/main/java/com/mall/admin/exam/AdminExamRecordController.java`
- Create: `mall-admin-app/src/test/java/com/mall/admin/exam/AdminExamControllerMvcTest.java`
- Modify: `mall-admin-app/src/test/java/com/mall/admin/architecture/AdminControllerRoutePolicyTest.java`

**Interfaces:**
- Produces: 设计规格 8.2 节列出的全部 `/admin/api/exam/**` GET/POST 接口。
- Consumes: Tasks 2–3 的治理、导入和查询 Service。

- [ ] **Step 1: 写管理员身份和路由契约失败测试**

```java
mockMvc.perform(post("/admin/api/exam/sources/review")
        .principal(() -> "admin")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"sourceId\":9,\"approved\":true,\"reviewNote\":\"授权材料已核验\"}"))
        .andExpect(status().isOk());

mockMvc.perform(get("/admin/api/exam/questions").param("page", "1").param("size", "20"))
        .andExpect(status().isOk());
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-admin-app -Dtest=AdminExamControllerMvcTest test`

Expected: FAIL，路由为 404。

- [ ] **Step 3: 实现 Controller 与分页查询**

Controller 使用 `@AuthenticationPrincipal AdminPrincipal` 取得管理员 ID；分页参数统一 `page >= 1`、`1 <= size <= 100`；导入预览使用 `MultipartFile`，其余写接口使用 `@Valid @RequestBody`。

- [ ] **Step 4: 更新路由策略测试**

将五个新 Controller 加入扫描，断言无 PUT、PATCH、DELETE、路径变量或重复映射。

- [ ] **Step 5: 运行管理应用测试**

Run: `mvn -pl mall-admin-app -am test`

Expected: PASS，匿名请求为 401，管理员请求使用统一 `ApiResponse`。

- [ ] **Step 6: 提交**

```bash
git add mall-exam mall-admin-app
git commit -m "feat: expose exam administration APIs"
```

---

### Task 5: 实现原子题组随机组卷和不可变试卷快照

**Files:**
- Modify: `mall-exam/pom.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/model/ExamPaperEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/model/ExamPaperGroupSnapshotEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/model/ExamPaperItemSnapshotEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/mapper/ExamPaperMapper.java`
- Create: `mall-exam/src/main/resources/mapper/exam/ExamPaperMapper.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/service/ExamPaperAssembler.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/service/ExamPaperService.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/lock/ExamGenerationLock.java`
- Create: `mall-exam/src/main/java/com/mall/exam/paper/lock/RedisExamGenerationLock.java`
- Create: `mall-exam/src/test/java/com/mall/exam/paper/service/ExamPaperAssemblerTest.java`
- Create: `mall-exam/src/test/java/com/mall/exam/paper/service/ExamPaperServiceTest.java`

**Interfaces:**
- Produces: `ExamPaperResponse ExamPaperService.generateOrResume(long userId)`。
- Produces: `ExamPaperResponse ExamPaperService.current(long userId)`。
- Produces: `ExamPaperAssembler.assemble(ExamBlueprintDefinition, QuestionPool, Set<Long> previouslyUsedIds)`。
- Consumes: Task 1 蓝图、Task 2 可用题库。

- [ ] **Step 1: 写题组原子性和题量失败测试**

```java
@Test
void assemblesExactBlueprintWithoutSplittingGroups() {
    AssembledPaper paper = assembler.assemble(ExamBlueprintDefinition.degreeEnglish2016V2(), pool(), usedIds());
    assertEquals(3, paper.groupsOfType("DIALOGUE").size());
    assertEquals(10, paper.itemsOfType("DIALOGUE_BLANK").size());
    assertEquals(4, paper.groupsOfType("READING").size());
    assertTrue(paper.groups().stream().allMatch(group -> group.containsAllMemberQuestions()));
    assertEquals(52, paper.items().size());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-exam -Dtest=ExamPaperAssemblerTest test`

Expected: FAIL，因为组卷器不存在。

- [ ] **Step 3: 实现纯内存组卷器**

组卷器先按蓝图校验容量，再随机打散候选；候选排序时把用户未做内容放在前面。对话选择三组且空格总数必须恰为 10；阅读选择四组且每组必须恰为五题。任何部分不足时抛出 `EXAM_BANK_INSUFFICIENT`，不返回部分结果。

- [ ] **Step 4: 写并实现事务快照与恢复测试**

测试 `generateOrResume` 在已有 `IN_PROGRESS` 时不插入新试卷；首次生成时在一个事务中插入 paper、group snapshots、item snapshots；Redis key 固定为 `mall:exam:generate:user:{userId}`，锁超时 10 秒并在 `finally` 释放。

- [ ] **Step 5: 运行模块测试**

Run: `mvn -pl mall-exam test`

Expected: PASS，连续 100 次组卷均满足蓝图且单卷无重复。

- [ ] **Step 6: 提交**

```bash
git add mall-exam
git commit -m "feat: generate immutable exam paper snapshots"
```

---

### Task 6: 实现答案版本、超时、判分、错题与主观题自评

**Files:**
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/model/ExamAnswerEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/model/ExamWrongRecordEntity.java`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/mapper/ExamAttemptMapper.java`
- Create: `mall-exam/src/main/resources/mapper/exam/ExamAttemptMapper.xml`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/service/ExamAttemptService.java`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/service/ExamScoringService.java`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/dto/SaveAnswerRequest.java`
- Create: `mall-exam/src/main/java/com/mall/exam/attempt/dto/SelfScoreRequest.java`
- Create: `mall-exam/src/test/java/com/mall/exam/attempt/service/ExamAttemptServiceTest.java`
- Create: `mall-exam/src/test/java/com/mall/exam/attempt/service/ExamScoringServiceTest.java`

**Interfaces:**
- Produces: `SavedAnswerResponse save(long userId, SaveAnswerRequest request)`。
- Produces: `ExamResultResponse submit(long userId, long paperId, boolean automatic)`。
- Produces: `ExamReviewResponse review(long userId, long paperId)`。
- Produces: `ExamResultResponse selfScore(long userId, SelfScoreRequest request)`。
- Consumes: Task 5 的 paper 与 snapshot Mapper。

- [ ] **Step 1: 写答案版本冲突失败测试**

```java
@Test
void rejectsStaleAnswerVersionAndReturnsLatestAnswer() {
    when(mapper.findAnswerForUpdate(8L, 44L)).thenReturn(answer("B", 3));
    BusinessException error = assertThrows(BusinessException.class,
            () -> service.save(7L, request(8L, 44L, "C", 2)));
    assertEquals(ErrorCode.EXAM_ANSWER_CONFLICT, error.getErrorCode());
    verify(mapper, never()).updateAnswer(any());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-exam -Dtest=ExamAttemptServiceTest,ExamScoringServiceTest test`

Expected: FAIL，因为答题 Service 不存在。

- [ ] **Step 3: 实现保存与截止时间检查**

注入现有 `Clock`。每次 `current`、`save`、`submit` 都比较 `Instant.now(clock)` 与 `expiresAt`；过期时先调用同一 `submit(..., true)`。保存成功后版本加一，已提交时抛出 `EXAM_ALREADY_SUBMITTED`。

- [ ] **Step 4: 实现幂等交卷和 70 分制判分**

Mapper 以 `SELECT ... FOR UPDATE` 锁定试卷。判分只比较四类客观题快照答案；错误或未答写入一条 `exam_wrong_record`。已提交试卷直接读取既有结果，不再次写错题。

```java
int score = objectiveItems.stream()
        .filter(item -> Objects.equals(normalize(item.getCorrectAnswer()), normalize(answerOf(item))))
        .mapToInt(ExamPaperItemSnapshotEntity::getScore)
        .sum();
```

- [ ] **Step 5: 实现复盘锁和自评分边界**

`review` 对 `IN_PROGRESS` 抛出 `EXAM_REVIEW_LOCKED`；响应只包含错题和未答题。`selfScore` 只接受两个 0–15 整数，并保存 `objectiveScore + translationScore + writingScore`。

- [ ] **Step 6: 运行测试**

Run: `mvn -pl mall-exam test`

Expected: PASS，覆盖提前交卷、超时交卷、重复提交、归属校验、错题生成和自评边界。

- [ ] **Step 7: 提交**

```bash
git add mall-exam
git commit -m "feat: save and score exam attempts"
```

---

### Task 7: 暴露考生 API 并验证答案不可泄露

**Files:**
- Create: `mall-shop-app/src/main/java/com/mall/exam/controller/ExamController.java`
- Create: `mall-shop-app/src/test/java/com/mall/exam/controller/ExamControllerMvcTest.java`
- Modify: `mall-shop-app/src/test/java/com/mall/architecture/ControllerRoutePolicyTest.java`
- Modify: `mall-shop-app/src/test/java/com/mall/security/SecurityAndMethodPolicyMvcTest.java`
- Modify: `README.md`

**Interfaces:**
- Produces: 规格 8.1 节定义的八个 `/api/exams/**` 接口。
- Consumes: Tasks 5–6 的 `ExamPaperService` 和 `ExamAttemptService`。

- [ ] **Step 1: 写认证、归属和敏感字段失败测试**

```java
mockMvc.perform(get("/api/exams/current").with(authentication(userAuthentication(7L))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].correctAnswer").doesNotExist())
        .andExpect(jsonPath("$.data.items[0].explanation").doesNotExist())
        .andExpect(jsonPath("$.data.items[0].referenceAnswer").doesNotExist());
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-shop-app -Dtest=ExamControllerMvcTest test`

Expected: FAIL，路由为 404。

- [ ] **Step 3: 实现 Controller**

全部方法使用 `@AuthenticationPrincipal AuthenticatedUser user`，从 `user.getUserId()` 传递身份；`paperId` 只通过 `@RequestParam` 或 JSON Body 接收；分页范围与现有接口一致。

- [ ] **Step 4: 更新安全和路由策略测试**

断言匿名访问考试 API 为 401、PUT/DELETE/OPTIONS 为 405、无路径变量；将运行中试卷响应序列化后扫描以下字段名并全部断言不存在：`correctAnswer`、`explanation`、`referenceAnswer`、`sampleEssay`、`scoringRubric`。

- [ ] **Step 5: 更新 README 接口表和运行说明**

记录八个考生接口、十五个管理接口、120 分钟规则和代理要求；明确初始内容为原创模拟题。

- [ ] **Step 6: 运行后端全量测试**

Run: `mvn test`

Expected: PASS，所有 Maven 模块零失败。

- [ ] **Step 7: 提交**

```bash
git add mall-shop-app README.md
git commit -m "feat: expose authenticated exam APIs"
```

---

### Task 8: 导入五套原创题库并验证完整性

**Files:**
- Modify: `mall-database/pom.xml`
- Create: `mall-database/src/main/java/db/migration/V13__seed_degree_english_exam.java`
- Create: `mall-database/src/main/resources/db/seed/degree-english-v1/set-01.json`
- Create: `mall-database/src/main/resources/db/seed/degree-english-v1/set-02.json`
- Create: `mall-database/src/main/resources/db/seed/degree-english-v1/set-03.json`
- Create: `mall-database/src/main/resources/db/seed/degree-english-v1/set-04.json`
- Create: `mall-database/src/main/resources/db/seed/degree-english-v1/set-05.json`
- Create: `mall-database/src/test/java/com/mall/database/exam/ExamSeedDatasetTest.java`

**Interfaces:**
- Produces: 数据集版本 `degree-english-v1`，五个文件每个都可独立组成一套完整试卷。
- Consumes: V12 表结构和 Task 3 的字段定义。

- [ ] **Step 1: 写数据集完整性失败测试**

```java
@ParameterizedTest
@ValueSource(strings = {"set-01.json", "set-02.json", "set-03.json", "set-04.json", "set-05.json"})
void eachSeedFileContainsOneCompletePaper(String filename) throws Exception {
    SeedSet set = loader.load("db/seed/degree-english-v1/" + filename);
    assertEquals(3, set.dialogues().size());
    assertEquals(10, set.dialogueQuestionCount());
    assertEquals(4, set.readings().size());
    assertTrue(set.readings().stream().allMatch(reading -> reading.questions().size() == 5));
    assertEquals(10, set.vocabulary().size());
    assertEquals(10, set.grammar().size());
    assertEquals(1, set.translation().size());
    assertEquals(1, set.writing().size());
    assertTrue(set.allObjectiveQuestionsHaveAnswerAndExplanation());
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `mvn -pl mall-database -Dtest=ExamSeedDatasetTest test`

Expected: FAIL，因为数据文件不存在。

- [ ] **Step 3: 转换现有原创样卷为 set-01**

从 `/Users/chengchengzi/Documents/ChatGPT/cet4/tools/build_mock_exam.py` 迁移已有题目、答案、解析、参考译文和范文；来源固定为 `ORIGINAL_SIMULATION_2026`，版权状态 `ORIGINAL`、审核状态 `APPROVED`。

- [ ] **Step 4: 编写 set-02 至 set-05**

每套使用不同主题和题干，不复制第三方真题；阅读总词数保持 1050–1300，英译汉原文 105–135 词，作文要求不少于 100 词。每个客观题都有唯一 A–D 选项、正确答案、中文解析、考点和难度。

- [ ] **Step 5: 实现 Flyway Java 迁移**

V13 通过 Jackson 读取五个 classpath JSON 文件，使用 `PreparedStatement` 批量写入一个已审核原创题源、蓝图、六个蓝图部分、题组和题目；迁移完全由 Flyway 版本保证只执行一次，不在应用启动代码中重复判断。

- [ ] **Step 6: 扩展数据质量测试**

断言 15 组对话/50 空、20 篇阅读/100 题、50 词汇、50 语法、5 翻译、5 写作，总计 260 个答题项；所有指纹唯一，五套答案分布中 A/B/C/D 各自占比均在 15%–35%。

- [ ] **Step 7: 运行数据库和后端全量测试**

Run: `mvn -pl mall-database test && mvn test`

Expected: PASS，五套数据质量断言和全部后端测试通过。

- [ ] **Step 8: 提交**

```bash
git add mall-database
git commit -m "feat: seed five original degree English exams"
```

---

### Task 9: 创建 examPage、用户认证和受保护路由

**Files:**
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/package.json`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/vite.config.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/vitest.config.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/tsconfig.json`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/main.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/App.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/router/index.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/http.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/auth-api.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/auth-session.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/stores/auth.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/LoginView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/http.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/router/index.spec.ts`

**Interfaces:**
- Produces: `apiRequest<T>(config)`，base URL `/api`，支持商城 Access/Refresh Token 自动轮换。
- Produces: `useAuthStore()` 和需要登录的 `/`、`/exam`、`/result/:paperId`、`/records` 路由。
- Consumes: 现有 `/api/auth/login`、`/api/auth/refresh`、`/api/auth/logout`、`/api/users/me`。

- [ ] **Step 1: 先创建测试配置与认证失败测试**

```ts
it('refreshes once and retries concurrent authenticated requests', async () => {
  saveTokenResponse(tokens('expired', 'refresh-one'), Date.now() - 2_000_000)
  mock.onPost('/auth/refresh').reply(200, ok(tokens('fresh', 'refresh-two')))
  mock.onGet('/users/me').reply(200, ok({ id: 1, username: 'learner' }))
  await Promise.all([apiRequest({ method: 'GET', url: '/users/me' }), apiRequest({ method: 'GET', url: '/users/me' })])
  expect(mock.history.post).toHaveLength(1)
})
```

- [ ] **Step 2: 安装依赖并确认 RED**

Run: `npm install && npm run test -- --run src/services/http.spec.ts`

Expected: FAIL，因为 HTTP 客户端和会话代码不存在。

- [ ] **Step 3: 实现最小工程和认证流程**

依赖版本与 `mallManagePage` 对齐；Vite 使用端口 5174，把 `/api` 代理到 `http://127.0.0.1:8080`。会话采用与管理端一致的 Access/Refresh Token 轮换模式，但 storage key 使用 `degree-english-exam-session`。

- [ ] **Step 4: 实现路由守卫和登录页**

未登录进入受保护路由时跳到 `/login?redirect=...`；已登录访问登录页时回到安全的站内 redirect。登录失败显示后端中文错误，成功后读取 `/api/users/me`。

- [ ] **Step 5: 运行测试、类型检查和 Lint**

Run: `npm run test && npm run typecheck && npm run lint`

Expected: PASS，认证与路由测试零失败。

- [ ] **Step 6: 初始化并提交 examPage 仓库**

```bash
git init -b main
git add .
git commit -m "feat: initialize degree English exam web app"
```

---

### Task 10: 实现一键组卷、专注逐题答题和可靠自动保存

**Files:**
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/types/exam.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/exam-api.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/stores/exam.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/stores/pending-answers.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamHomeView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamTakingView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/exam/ExamHeader.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/exam/SectionNavigation.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/exam/QuestionPanel.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/exam/AnswerSheet.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/exam/SubmitConfirmDialog.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/stores/exam.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamTakingView.spec.ts`

**Interfaces:**
- Produces: `generateExam`、`currentExam`、`saveAnswer`、`submitExam` API 函数。
- Produces: `useExamStore`，包含 `loadOrGenerate()`、`answer(itemId, value)`、`flush()`、`submit(automatic)`。
- Consumes: Task 7 考生 API。

- [ ] **Step 1: 写刷新恢复和倒计时失败测试**

```ts
it('uses server expiry and restores saved answers', async () => {
  vi.setSystemTime('2026-09-18T02:00:00Z')
  mockCurrentExam(exam({ expiresAt: '2026-09-18T04:00:00Z', answers: [{ itemId: 11, answer: 'B', version: 2 }] }))
  const store = useExamStore()
  await store.restore()
  expect(store.answerFor(11)).toBe('B')
  expect(store.remainingSeconds).toBe(7200)
})
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `npm run test -- --run src/stores/exam.spec.ts src/views/ExamTakingView.spec.ts`

Expected: FAIL，因为考试 Store 和页面不存在。

- [ ] **Step 3: 实现 API、Store 与离线队列**

答案变化 800ms 防抖保存；切题和提交前 `flush()`。失败请求保留在 localStorage key `degree-english-pending-answers:{paperId}`，联网后按题目顺序重试。`EXAM_ANSWER_CONFLICT` 展示本地/服务器答案选择，不静默覆盖。

- [ ] **Step 4: 实现确认过的专注逐题布局**

顶部固定考试名称、保存状态和倒计时；左侧章节进度；中央题面；右侧答题卡。阅读文章在同题组五题间保持可见，对话共享选项；支持题号跳转、上一题、保存并下一题和标记复查。

- [ ] **Step 5: 实现超时和提交确认**

手动提交显示未答数量并二次确认；倒计时归零立即调用 `submit(true)`，不弹确认。提交中页面只读，失败后用相同 `paperId` 重试直到得到确定结果。

- [ ] **Step 6: 运行前端检查**

Run: `npm run test && npm run typecheck && npm run lint && npm run build`

Expected: PASS，生产构建成功且无 TypeScript 错误。

- [ ] **Step 7: 提交**

```bash
git add src package.json package-lock.json
git commit -m "feat: deliver timed exam taking experience"
```

---

### Task 11: 实现成绩、错题复盘、主观题自评和考试历史

**Files:**
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamResultView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamRecordsView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/review/ScoreSummary.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/review/WrongQuestionCard.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/components/review/SubjectiveSelfScore.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamResultView.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/views/ExamRecordsView.spec.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/router/index.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/examPage/src/services/exam-api.ts`

**Interfaces:**
- Produces: `getExamReview(paperId)`、`listExamRecords(query)`、`saveSelfScore(payload)`。
- Consumes: Task 7 的 review、records、self-score 接口。

- [ ] **Step 1: 写只展示错题和自评总分失败测试**

```ts
it('renders wrong answers and recalculates total after self scoring', async () => {
  mockReview(review({ objectiveScore: 58, wrongItems: [wrongReading()], translationScore: null, writingScore: null }))
  const wrapper = mountResultRoute()
  await flushPromises()
  expect(wrapper.text()).toContain('58 / 70')
  expect(wrapper.findAll('[data-test="wrong-question"]')).toHaveLength(1)
  await wrapper.get('[data-test="translation-score"]').setValue('12')
  await wrapper.get('[data-test="writing-score"]').setValue('11')
  await wrapper.get('[data-test="save-self-score"]').trigger('click')
  expect(wrapper.text()).toContain('自评总分 81')
})
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `npm run test -- --run src/views/ExamResultView.spec.ts src/views/ExamRecordsView.spec.ts`

Expected: FAIL，结果与记录页面不存在。

- [ ] **Step 3: 实现结果和错题解析页面**

按确认稿展示客观题分数、正确/错误数、用时、错题分布；每张错题卡显示用户答案、正确答案、解析和考点。客户端再次过滤 `isCorrect === false`，若后端误返回正确题则不渲染并记录非敏感控制台警告。

- [ ] **Step 4: 实现主观题自评**

翻译展示用户答案、参考译文、关键表达和四维量表；写作展示用户答案、字数、写作思路、分档标准和范文。输入限制为整数 0–15，保存成功后显示 `objectiveScore + translationScore + writingScore`。

- [ ] **Step 5: 实现考试历史**

历史按提交时间倒序分页，显示试卷编号、状态、开始/提交时间、客观成绩和自评总分；点击进入相同复盘页面。

- [ ] **Step 6: 运行检查并提交**

Run: `npm run test && npm run typecheck && npm run lint && npm run build`

```bash
git add src
git commit -m "feat: add exam review and history"
```

---

### Task 12: 扩展 mallManagePage 的考试管理能力

**Files:**
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/types/exam.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/services/exam-api.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/services/exam-api.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionSourceView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionBankView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionEditorView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionImportView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/ExamBlueprintView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/ExamRecordView.vue`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionBankView.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/views/exam/QuestionImportView.spec.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/router/index.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/router/index.spec.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/src/layouts/AdminLayout.vue`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/README.md`

**Interfaces:**
- Produces: 规格 8.2 的管理 API TypeScript 客户端。
- Consumes: Task 4 管理 API。

- [ ] **Step 1: 写 API 路径与路由失败测试**

```ts
it('reviews a source with POST and lists questions with GET', async () => {
  mock.onPost('/exam/sources/review').reply(200, ok(source()))
  mock.onGet('/exam/questions').reply(200, ok(page([question()])))
  await reviewQuestionSource({ sourceId: 9, approved: true, reviewNote: '授权材料已核验' })
  await listExamQuestions({ page: 1, size: 20 })
  expect(mock.history.post[0].url).toBe('/exam/sources/review')
  expect(mock.history.get[0].url).toBe('/exam/questions')
})
```

- [ ] **Step 2: 运行测试并确认 RED**

Run: `npm run test -- --run src/services/exam-api.spec.ts src/router/index.spec.ts`

Expected: FAIL，考试管理 API 和路由不存在。

- [ ] **Step 3: 实现类型、API、路由与菜单**

增加 `/exam/sources`、`/exam/questions`、`/exam/questions/new`、`/exam/questions/:questionId/edit`、`/exam/imports`、`/exam/blueprints`、`/exam/records`；路径变量仅存在前端路由，后端请求仍使用 query/body ID。侧栏以“考试管理”分组显示菜单。

- [ ] **Step 4: 实现题源和题库页面**

来源审核必须选择版权状态并填写意见。题库支持题型、审核状态、启停状态、考点和关键词筛选；对话/阅读编辑器一次提交正文、共享选项和全部小题，禁止拆分发布。

- [ ] **Step 5: 实现导入、蓝图和记录页面**

导入页先上传并展示总数、有效数、重复数和逐行错误，只有零结构错误时才允许提交；蓝图页展示各部分需求、可用容量和最多完整套数；记录页不渲染答案正文。

- [ ] **Step 6: 写页面行为测试**

覆盖未授权来源的发布按钮禁用、重复预警、题组整体编辑、导入错误阻止提交、蓝图容量不足警告和记录列表无答案字段。

- [ ] **Step 7: 运行管理端完整检查**

Run: `npm run typecheck && npm run lint && npm run test && npm run build`

Expected: PASS，所有检查零失败。

- [ ] **Step 8: 提交**

```bash
git add src README.md
git commit -m "feat: manage exam sources and question bank"
```

---

### Task 13: 跨仓库端到端验收、部署代理与最终文档

**Files:**
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/playwright.config.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/e2e/exam-flow.spec.ts`
- Create: `/Users/chengchengzi/Documents/ChatGPT/examPage/e2e/exam-layout.spec.ts`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/examPage/README.md`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallService/README.md`
- Modify: `/Users/chengchengzi/Documents/ChatGPT/mallManagePage/README.md`

**Interfaces:**
- Consumes: Tasks 1–12 的后端、考生端和管理端完整能力。
- Produces: 可重复执行的端到端验收和三仓库运行说明。

- [ ] **Step 1: 写完整流程 E2E 测试**

```ts
test('login, generate, resume, submit, review and self-score', async ({ page }) => {
  await loginAsSeedUser(page)
  await page.getByRole('button', { name: '一键生成模拟试卷' }).click()
  await answerObjectiveQuestion(page, 1, 'B')
  await page.reload()
  await expect(page.getByText('已自动恢复')).toBeVisible()
  await page.getByRole('button', { name: '提交试卷' }).click()
  await confirmSubmission(page)
  await expect(page.getByText(/客观题得分/)).toBeVisible()
  await page.getByLabel('英译汉自评分').fill('12')
  await page.getByLabel('短文写作自评分').fill('11')
  await page.getByRole('button', { name: '保存自评分' }).click()
  await expect(page.getByText(/自评总分/)).toBeVisible()
})
```

- [ ] **Step 2: 运行 E2E 并确认 RED**

Run: `npm run test:e2e`

Expected: 首次 FAIL，直到真实后端、数据库、Redis 和两个前端连通。

- [ ] **Step 3: 配置本地与生产代理说明**

`examPage` 的 `/api` 指向 8080；`mallManagePage` 的 `/admin/api` 指向 8081。README 给出 MySQL、Redis、两个 Spring Boot 应用和两个 Vite 应用的启动顺序，生产环境明确要求同源 HTTPS 反向代理。

- [ ] **Step 4: 增加布局与敏感字段 E2E 断言**

在 1024、1280、1440、1920 四种宽度断言页面无横向滚动且提交按钮可见；拦截运行中试卷响应并断言敏感字段名不存在；交卷后断言只渲染错误/未答题卡。

- [ ] **Step 5: 运行最终验证矩阵**

Run in `mallService`: `mvn test && mvn package -DskipTests`

Run in `examPage`: `npm run typecheck && npm run lint && npm run test && npm run build && npm run test:e2e`

Run in `mallManagePage`: `npm run typecheck && npm run lint && npm run test && npm run build`

Expected: 三组命令全部退出码 0，JUnit/Vitest/Playwright 零失败。

- [ ] **Step 6: 对照规格逐项验收**

逐条核对设计规格第 13.4 节十项验收标准；把验证命令、测试数和结果记录在最终交付说明中，不以单一构建成功替代功能验收。

- [ ] **Step 7: 提交文档与 E2E**

```bash
# examPage
git add playwright.config.ts e2e README.md
git commit -m "test: cover complete exam workflow"

# mallService
git add README.md
git commit -m "docs: document exam service APIs"

# mallManagePage
git add README.md
git commit -m "docs: document exam administration"
```

---

## Execution Order and Review Gates

1. Tasks 1–4：先完成题库治理与管理 API；审核后再进入组卷。
2. Tasks 5–8：完成核心考试域与五套内容；必须通过后端全量测试后再开发考生端。
3. Tasks 9–11：完成电脑考试端；必须通过类型检查、单测和构建。
4. Task 12：完成管理后台页面。
5. Task 13：只在前三个仓库功能均完成后执行跨仓库验收。

每个 Gate 都检查当前仓库 `git diff`、测试输出和规格覆盖，发现回归时留在当前 Gate 修复，不把失败推迟到最终任务。

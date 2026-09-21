# Fifty Fixed Degree-English Papers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans task-by-task. User direction overrides the normal test/review gates: implementation only, no tests, review, build, or full-reactor verification.

**Goal:** Publish fifty fixed, original degree-English simulation papers that can be selected and started without changing the existing review workflow.

**Architecture:** Add immutable fixed-paper template tables and clone a chosen template into the existing user-owned paper snapshots. Five forward Java Flyway migrations load ten independently authored JSON papers each; student and admin APIs expose templates without leaking answers before submission.

**Tech Stack:** Java 11, Spring Boot 2.7, MyBatis, Flyway, MySQL 8, Vue 3, TypeScript.

**Spec:** `docs/superpowers/specs/2026-09-21-fifty-fixed-degree-english-papers-design.md`

## Global Constraints

- `fixed-01.json` through `fixed-50.json` are UTF-8, original simulation content and never copied official or third-party questions.
- Each fixed paper has 3 dialogue groups/10 blanks, 4 readings/20 items, 10 vocabulary, 10 grammar, 1 translation and 1 writing item; total 100 points/120 minutes.
- Correct answers, explanations, references, samples and rubrics never appear in an in-progress student response.
- Existing V12/V13 migrations and learner attempts are immutable; all schema/content changes are forward migrations.
- Do not add/run tests, reviews, builds, E2E, or reactor verification unless the user revokes this direction.

## Review Focus

- A template whose ordered items do not exactly satisfy the blueprint must be rejected before publication.
- A user with an in-progress paper cannot start a second template paper.
- Template answers and subjective references must not be serialized by current-paper endpoints.
- Migration retries after a failure must not leave partially published fixed templates.
- Editing a published template must create a new version rather than mutate an existing learner snapshot.

---

### Task 1: Fixed-paper schema and template model

**Files:** create V15 forward schema migration; create template entities, mappers and service DTOs in `mall-exam`.

**Interfaces:** produce `FixedPaperTemplate`, `FixedPaperTemplateItem`, `FixedPaperTemplateGroup`, and `FixedPaperTemplateRepository.findPublished(String paperCode)`.

- [ ] Create `exam_fixed_paper`, `exam_fixed_paper_group` and `exam_fixed_paper_item` with immutable code/version, ordered snapshots, source fingerprints and foreign keys to blueprint/source content.
- [ ] Add mapper XML and Java model/repository methods for published-template lookup and ordered template loading.
- [ ] Commit: `feat: add fixed paper template domain`.

### Task 2: Selected-template attempt creation

**Files:** modify `ExamPaperService`, request/response DTOs and student exam controller.

**Interfaces:** consume `FixedPaperTemplateRepository`; produce `generateFixedPaper(Long userId, String paperCode)` and `listFixedPapers()`.

- [ ] Add a selected-paper generation request; validate enabled template, one active attempt per user, and blueprint structure before inserting a user-owned `exam_paper`.
- [ ] Clone the template’s ordered group/item snapshots into existing snapshot tables in one transaction; preserve all answer/reference fields server-side only.
- [ ] Add template list and selection endpoints; retain the existing random generation endpoint for configured random mode.
- [ ] Commit: `feat: generate attempts from fixed templates`.

### Task 3: Original fixed-paper content batches 01–20

**Files:** create `fixed-01.json`–`fixed-20.json`; create V16 and V17 Java seed migrations plus shared immutable JSON loader.

**Interfaces:** each file produces one complete fixed template with an original fingerprint manifest; migrations publish ten files atomically per batch.

- [ ] Author twenty complete papers with unique dialogue, reading, vocabulary, grammar, translation and writing materials.
- [ ] Validate content count and fixed order inside the loader before any database insert; reject duplicate content fingerprints within/between batches.
- [ ] Persist source declaration, questions, template groups/items and fingerprint reservations transactionally.
- [ ] Commit: `feat: seed first twenty fixed simulation papers`.

### Task 4: Original fixed-paper content batches 21–40

**Files:** create `fixed-21.json`–`fixed-40.json`; create V18 and V19 Java seed migrations.

**Interfaces:** same loader and stable codes as Task 3.

- [ ] Author the next twenty complete original papers, varying adult study, work, public-service, science and community contexts.
- [ ] Load in two ten-paper forward-migration transactions and preserve published-paper immutability.
- [ ] Commit: `feat: seed fixed simulation papers twenty one to forty`.

### Task 5: Original fixed-paper content batches 41–50 and examPage selection

**Files:** create `fixed-41.json`–`fixed-50.json`; create V20 migration; modify `examPage` types/API/home view.

**Interfaces:** consumes `listFixedPapers()` and `generateFixedPaper(paperCode)`; produces a numbered fixed-paper picker.

- [ ] Author the final ten complete original papers and load them in one transaction.
- [ ] Replace the home screen’s implicit generation action with a list/picker that defaults to the next unused fixed paper and clearly labels fixed original simulations.
- [ ] Route an existing in-progress attempt back to it instead of allowing another paper selection.
- [ ] Commit: `feat: offer fifty fixed simulation papers`.

### Task 6: Administration, documentation and migration operational guidance

**Files:** modify admin typed client/routes/views; modify three README files.

**Interfaces:** consumes admin fixed-paper list/status endpoints; produces read-only template inventory and publication state.

- [ ] Add read-only fixed-template inventory with code, version, blueprint, item counts, source declaration and publication state; only allow status changes that do not mutate snapshots.
- [ ] Document the five forward seed batches, original-content restriction, database backup expectation before migration, and fixed-paper selection behavior.
- [ ] Commit: `docs: document fixed simulation paper publishing`.

## Execution Handoff

Plan complete. It intentionally separates domain behavior from three content batches so each migration can be reviewed and recovered independently. Please review it and confirm it captures what you want before implementation.

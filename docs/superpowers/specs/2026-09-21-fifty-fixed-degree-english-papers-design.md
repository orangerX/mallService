# Fifty fixed original degree-English simulation papers

## Goal

Add fifty independently authored, fixed simulation papers for the 2016 second-edition non-English-major degree-English outline. Each paper has a stable paper code, complete answer key and explanations, a translation reference, a writing sample, and scoring rubrics. The materials are original practice content, never copied or presented as past official examinations.

## Why the current seed is insufficient

The existing five seed sets populate a random-selection question pool. `exam_paper` is user-owned and created at attempt time, so it cannot represent a reusable, fixed published paper. Simply adding more questions would improve capacity but would not guarantee that a learner receives one coherent predefined paper.

## Data model

Add a published-paper-template layer:

- `exam_fixed_paper`: immutable paper identity, code, title, blueprint and display order.
- `exam_fixed_paper_group`: ordered dialogue/reading material belonging to one fixed paper.
- `exam_fixed_paper_item`: ordered objective and subjective item references plus their frozen question snapshot.

Templates are source-owned rather than user-owned. Starting a paper clones its fixed group/item snapshots into the existing `exam_paper` / `exam_paper_*_snapshot` tables, preserving the current answer, grading, review, and self-score workflow. The student API exposes the selected fixed paper code only after generation; correct answers and explanations remain excluded until submission.

## Content and import format

Create fifty standalone UTF-8 JSON files, `fixed-01.json` through `fixed-50.json`. Each contains exactly:

| Section | Fixed-paper content |
| --- | --- |
| Dialogue | 3 groups, 10 blanks |
| Reading | 4 passages, 20 questions |
| Vocabulary | 10 questions |
| Grammar | 10 questions |
| Translation | 1 passage, Chinese reference, rubric |
| Writing | 1 prompt, model answer, rubric |

Every file has a content fingerprint, original-source declaration, and no reused passage, dialogue, item stem, reference translation, or writing prompt across the fifty papers. Topics remain adult-learning and public-life oriented; difficulty mixes 2–4 with an overall intermediate degree-English level.

## Migration and delivery

Use five forward Flyway Java migrations, each loading ten files, to keep transactions, failures, and review diffs manageable. A migration inserts source records, question content, fixed templates, and fingerprint reservations atomically. Existing V13 and currently applied schemas are not modified. All five batches are idempotent only through Flyway history; a failed migration is repaired before rerun rather than silently skipped.

## Generation and user flow

The exam-home action changes from unrestricted random generation to a paper picker with a default “next unused fixed paper” choice. A learner may choose a numbered simulation paper. Existing in-progress-paper behavior remains authoritative: a user cannot start a second active paper. The original random mode can remain available only when explicitly selected by configuration; it is not used for the fifty fixed papers.

## Quality and safety constraints

- No internet scraping, official-paper copying, or third-party copyrighted questions.
- Correct-answer, explanation, translation-reference, model-answer, and rubric fields are stored only in server-side template/snapshots and omitted from in-progress responses.
- Published templates are immutable; corrections create a new template version rather than altering learner attempts.
- The requested implementation will preserve the previously stated execution policy: no new tests or review work unless the user changes that instruction.

## Acceptance criteria

1. The system lists exactly fifty fixed, selectable papers with stable codes.
2. Each paper matches the blueprint’s 100-point / 120-minute structure.
3. Starting a selected paper creates the existing user-specific immutable attempt snapshots.
4. In-progress responses do not leak answers or reference materials; submitted review retains current behavior.
5. All material is clearly labelled original simulation content.

package com.mall.exam.admin;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AdminExamQueryContractTest {
    @Test void recordQueryNeverSelectsAnswersAndOrdersPagesStably() throws Exception {
        String sql = sql("records", Collections.emptyMap());
        assertFalse(sql.contains("answer_content"));
        assertFalse(sql.contains("correct_answer"));
        assertFalse(sql.contains("SELECT *"));
        assertTrue(sql.contains("ORDER BY p.started_at DESC, p.id DESC"));
        assertTrue(sql.contains("LIMIT ? OFFSET ?"));
    }
    @Test void questionListFiltersAndCountSharePredicateWithoutAnswerProjection() throws Exception {
        Map<String,Object> filters = new HashMap<>();
        filters.put("questionType", "GRAMMAR"); filters.put("reviewStatus", "APPROVED");
        filters.put("enabled", 1); filters.put("sourceId", 9L); filters.put("keyword", "word"); filters.put("knowledgePoint", "tense");
        String page = sql("questions", filters), count = sql("countQuestions", filters);
        String predicate = count.substring(count.indexOf("WHERE"));
        assertTrue(page.contains(predicate));
        assertFalse(page.contains("correct_answer")); assertFalse(page.contains("reference_answer"));
        assertTrue(page.contains("q.question_type = ?")); assertTrue(page.contains("q.source_id = ?"));
        assertTrue(page.contains("ORDER BY q.id DESC"));
    }
    @Test void capacitySqlRequiresReusableSourcesAndCompleteCompatibleGroups() throws Exception {
        String capacity=sql("capacities",Collections.singletonMap("blueprintId",1L));
        assertTrue(capacity.contains("s.enabled=1"));assertTrue(capacity.contains("q.enabled!=1"));
        assertTrue(capacity.contains("eligible.item_count=5"));
        assertTrue(capacity.contains("q.source_id!=g.source_id"));
        String dialogue=sql("dialogueGroupCounts",Collections.emptyMap());
        assertTrue(dialogue.contains("GROUP BY eligible.item_count"));
        assertFalse(dialogue.contains("IN (3,4)"));
    }
    @Test void draftEditsAreGuardedAgainstPublicationAndSnapshotReferences() throws Exception {
        String question=sql("updateDraftQuestion",Collections.emptyMap()),group=sql("updateDraftGroup",Collections.emptyMap());
        assertTrue(question.contains("review_status IN ('DRAFT','PENDING','REJECTED')"));
        assertTrue(question.contains("original_question_id=?"));
        assertTrue(group.contains("original_group_id=?"));
        assertFalse(question.contains("SET id="));assertFalse(group.contains("SET id="));
        assertTrue(question.contains("group_id <=> ?"));
        assertTrue(question.contains("group_sort_order=?"));
        String deletion=sql("deleteDraftMember",Collections.emptyMap());
        assertTrue(deletion.contains("WHERE id=? AND group_id=?"));
        assertTrue(deletion.contains("review_status IN ('DRAFT','PENDING','REJECTED')"));
        assertTrue(deletion.contains("NOT EXISTS (SELECT 1 FROM exam_paper_item_snapshot WHERE original_question_id=?)"));
    }
    @Test void sourceSchemaKeepsEnablementSeparateFromReviewState() throws Exception {
        java.nio.file.Path module=java.nio.file.Path.of(System.getProperty("basedir",".")).toAbsolutePath();
        String schema=java.nio.file.Files.readString(module.resolve("../mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql"));
        String source=schema.substring(0,schema.indexOf("CREATE TABLE exam_import_batch"));
        assertTrue(source.contains("enabled TINYINT(1) NOT NULL DEFAULT 1"));
        assertTrue(source.contains("reviewed_by BIGINT UNSIGNED NULL"));
        assertTrue(source.contains("review_status ENUM("));
    }
    private String sql(String name, Map<String,Object> filters) throws Exception {
        Configuration c = new Configuration();
        for (String dependency : Arrays.asList("QuestionMapper", "QuestionSourceMapper")) {
            String resource = "mapper/exam/"+dependency+".xml";
            try(InputStream input=getClass().getClassLoader().getResourceAsStream(resource)) {
                new XMLMapperBuilder(input,c,resource,c.getSqlFragments()).parse();
            }
        }
        String path = "mapper/exam/AdminExamMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "Admin exam queries must be implemented");
            new XMLMapperBuilder(input,c,path,c.getSqlFragments()).parse();
        }
        Map<String,Object> params = new HashMap<>(filters); params.put("offset", 100L); params.put("size", 20);
        return c.getMappedStatement("com.mall.exam.question.mapper.AdminExamMapper."+name).getBoundSql(params).getSql().trim().replaceAll("\\s+"," ");
    }
}

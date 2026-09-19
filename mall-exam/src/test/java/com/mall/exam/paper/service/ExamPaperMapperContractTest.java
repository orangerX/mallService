package com.mall.exam.paper.service;
import com.mall.exam.paper.mapper.ExamPaperMapper;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ExamPaperMapperContractTest {
    Configuration configuration() throws Exception {
        Configuration c=new Configuration();
        for(String name:List.of("QuestionMapper","ExamPaperMapper")) {
            String path="mapper/exam/"+name+".xml";
            try(InputStream in=getClass().getClassLoader().getResourceAsStream(path)) {
                assertNotNull(in,"Paper mapper must be present");
                new XMLMapperBuilder(in,c,path,c.getSqlFragments()).parse();
            }
        }
        return c;
    }
    String sql(Configuration c,String name) {
        return c.getMappedStatement(ExamPaperMapper.class.getName()+"."+name).getBoundSql(Map.of("userId",7L,"paperId",9L,"code","DEGREE_ENGLISH_2016_V2")).getSql().replaceAll("\\s+"," ").trim();
    }
    @Test void onlyWholeAvailableGroupsAndReusableSourcesCanBeSelected() throws Exception {
        Configuration c=configuration();
        for(String query:List.of("findAvailableGroups","findAvailableQuestions")) {
            String s=sql(c,query);
            assertTrue(s.contains("s.enabled=1"));assertTrue(s.contains("s.review_status='APPROVED'"));
            assertTrue(s.contains("'ORIGINAL','AUTHORIZED','PUBLIC_OFFICIAL'"));
            assertTrue(s.contains("q.source_id!=g.source_id"));assertTrue(s.contains("q.enabled!=1"));
            assertTrue(s.contains("q.review_status!='APPROVED'"));assertTrue(s.contains("g.enabled=1"));
            assertTrue(s.contains("COUNT(DISTINCT q.group_sort_order)=COUNT(q.id)"));
            assertTrue(s.contains("COUNT(q.id)=5"));assertTrue(s.contains("BETWEEN 1 AND 8"));
        }
        String questions=sql(c,"findAvailableQuestions");
        assertTrue(questions.contains("q.group_id IS NULL"));assertTrue(questions.contains("q.group_id IN"));
    }
    @Test void snapshotInsertBindingsAndReadsMatchSchemaAndCarryGeneratedKeys() throws Exception {
        Configuration c=configuration();
        java.nio.file.Path module=java.nio.file.Path.of(System.getProperty("basedir",".")).toAbsolutePath();
        String schema=java.nio.file.Files.readString(module.resolve("../mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql"));
        for(String name:List.of("insertPaper","insertGroupSnapshot","insertItemSnapshot")) {
            MappedStatement statement=c.getMappedStatement(ExamPaperMapper.class.getName()+"."+name);
            assertInstanceOf(Jdbc3KeyGenerator.class,statement.getKeyGenerator());assertArrayEquals(new String[]{"id"},statement.getKeyProperties());
            String s=sql(c,name);String table=s.substring("INSERT INTO ".length(),s.indexOf(" ("));
            String tableSchema=schema.substring(schema.indexOf("CREATE TABLE "+table+" ("));
            tableSchema=tableSchema.substring(0,tableSchema.indexOf(") ENGINE"));
            String columns=s.substring(s.indexOf('(')+1,s.indexOf(')'));
            for(String column:columns.split(","))assertTrue(tableSchema.contains(column.trim()+" "),column);
        }
        String items=sql(c,"insertItemSnapshot");
        for(String field:List.of("correct_answer","explanation","reference_answer","sample_answer","scoring_rubric","knowledge_points","score"))assertTrue(items.contains(field));
        assertTrue(sql(c,"findItemSnapshots").contains("ORDER BY section_order,item_order,id"));
        assertTrue(sql(c,"findGroupSnapshots").contains("ORDER BY section_order,group_order,id"));
    }
    @Test void ownershipHistoryAndDatabaseLockRemainUserScoped() throws Exception {
        Configuration c=configuration();
        assertTrue(sql(c,"findInProgress").contains("user_id=? AND status='IN_PROGRESS'"));
        assertTrue(sql(c,"lockUser").endsWith("WHERE id=? FOR UPDATE"));
        for(String name:List.of("findPreviouslyUsedQuestionIds","findPreviouslyUsedGroupIds"))assertTrue(sql(c,name).contains("p.user_id=?"));
        assertTrue(sql(c,"findEnabledBlueprint").contains("status='ENABLED'"));
    }
}

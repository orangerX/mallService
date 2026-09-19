package com.mall.admin.exam;

import com.mall.admin.auth.controller.AdminAuthController;
import com.mall.admin.auth.service.AdminAuthService;
import com.mall.admin.auth.session.AdminSessionService;
import com.mall.admin.config.AdminSecurityConfig;
import com.mall.admin.security.AdminJwtTokenService;
import com.mall.admin.security.AdminTokenClaims;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AdminExamSourceController.class,AdminExamQuestionController.class,AdminExamImportController.class,
        AdminExamBlueprintController.class,AdminExamRecordController.class})
@Import(AdminSecurityConfig.class)
class AdminExamControllerMvcTest {
    @Autowired MockMvc mvc;
    @MockBean AdminAuthService auth;
    @MockBean AdminJwtTokenService tokens;
    @MockBean AdminSessionService sessions;
    @MockBean com.mall.exam.admin.service.AdminExamQueryService query;
    @MockBean com.mall.exam.admin.service.AdminExamManagementService management;
    @MockBean com.mall.exam.question.service.QuestionGovernanceService governance;
    @MockBean com.mall.exam.importer.ExamImportService imports;

    @org.junit.jupiter.api.BeforeEach void authenticate() {
        when(tokens.parseAccess("exam-admin")).thenReturn(new AdminTokenClaims(42L, "admin", "sid", "jti"));
        when(sessions.active("sid", 42L)).thenReturn(true);
    }

    @Test void sourceReviewRouteUsesAdminAuthentication() throws Exception {
        when(tokens.parseAccess("exam-admin")).thenReturn(new AdminTokenClaims(42L, "admin", "sid", "jti"));
        when(sessions.active("sid", 42L)).thenReturn(true);
        mvc.perform(post("/admin/api/exam/sources/review")
                .header("Authorization", "Bearer exam-admin").contentType("application/json")
                .content("{\"sourceId\":9,\"approved\":true,\"reviewNote\":\"verified\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
        org.mockito.Mockito.verify(governance).reviewSource(org.mockito.ArgumentMatchers.eq(9L),org.mockito.ArgumentMatchers.eq(42L),org.mockito.ArgumentMatchers.any());
    }

    @Test void anonymousReadAndWriteRequireAdminToken() throws Exception {
        for(String path:new String[]{"sources","questions","questions/detail","blueprints","records"})
            mvc.perform(get("/admin/api/exam/"+path)).andExpect(status().isUnauthorized());
        mvc.perform(post("/admin/api/exam/imports/commit").contentType("application/json").content("{\"batchId\":2}")).andExpect(status().isUnauthorized());
    }

    @Test void listPaginationRejectsEveryBoundaryViolation() throws Exception {
        for(String path:new String[]{"sources","questions","records"}) {
            for(String[] pair:new String[][]{{"page","0"},{"size","0"},{"size","101"},{"page","invalid"}})
                mvc.perform(get("/admin/api/exam/"+path).param(pair[0],pair[1]).header("Authorization","Bearer exam-admin"))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        }
    }

    @Test void questionFiltersAndRecordTotalsAreReturnedInStandardEnvelope() throws Exception {
        when(query.questions("GRAMMAR","APPROVED",1,9L,"tense","word",2,100))
            .thenReturn(new com.mall.common.api.PageResponse<>(java.util.Collections.emptyList(),2,100,123));
        mvc.perform(get("/admin/api/exam/questions").param("questionType","GRAMMAR").param("reviewStatus","APPROVED")
            .param("enabled","1").param("sourceId","9").param("knowledgePoint","tense").param("keyword","word")
            .param("page","2").param("size","100").header("Authorization","Bearer exam-admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(123)).andExpect(jsonPath("$.data.page").value(2));
        when(query.records(7L,"EX","SUBMITTED",1,1)).thenReturn(new com.mall.common.api.PageResponse<>(java.util.Collections.emptyList(),1,1,4));
        mvc.perform(get("/admin/api/exam/records").param("userId","7").param("paperNo","EX").param("status","SUBMITTED").param("size","1")
            .header("Authorization","Bearer exam-admin")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(4));
    }

    @Test void allMutationsUseAuthenticatedNumericAdminAndIgnoreForgedAdminId() throws Exception {
        String source="\"sourceName\":\"Book\",\"sourceType\":\"BOOK\",\"copyrightStatus\":\"ORIGINAL\"";
        String question="\"sourceId\":9,\"content\":{\"questionType\":\"GRAMMAR\",\"stem\":\"Question\",\"difficulty\":2}";
        for(String[] action:new String[][]{
            {"sources/create","{"+source+",\"adminId\":999}"},
            {"sources/update","{\"sourceId\":9,"+source+",\"adminId\":999}"},
            {"sources/status","{\"sourceId\":9,\"enabled\":0,\"adminId\":999}"},
            {"questions/create","{"+question+",\"adminId\":999}"},
            {"questions/update","{\"questionId\":8,"+question+",\"adminId\":999}"},
            {"questions/review","{\"questionId\":8,\"approved\":true,\"reviewNote\":\"verified\",\"adminId\":999}"},
            {"questions/status","{\"questionId\":8,\"enabled\":0,\"adminId\":999}"},
            {"blueprints/status","{\"blueprintId\":1,\"status\":\"DISABLED\",\"adminId\":999}"},
            {"imports/commit","{\"batchId\":2,\"adminId\":999}"}})
            mvc.perform(post("/admin/api/exam/"+action[0]).header("Authorization","Bearer exam-admin").contentType("application/json").content(action[1]))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"));
        org.mockito.Mockito.verify(management).createSource(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(42L));
        org.mockito.Mockito.verify(management).updateSource(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(42L));
        org.mockito.Mockito.verify(management).sourceStatus(9L,0,42L);
        org.mockito.Mockito.verify(management).createQuestion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(42L));
        org.mockito.Mockito.verify(management).updateQuestion(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.eq(42L));
        org.mockito.Mockito.verify(governance).reviewQuestion(org.mockito.ArgumentMatchers.eq(8L),org.mockito.ArgumentMatchers.eq(42L),org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(management).questionStatus(8L,0,42L);
        org.mockito.Mockito.verify(management).blueprintStatus(1L,"DISABLED",42L);
        org.mockito.Mockito.verify(imports).commit(2L,42L);
    }

    @Test void requiredBodiesIdsAndReviewDecisionAreValidated() throws Exception {
        for(String path:new String[]{"sources/create","sources/update","sources/review","sources/status","questions/create","questions/update","questions/review","questions/status","blueprints/status","imports/commit"})
            mvc.perform(post("/admin/api/exam/"+path).header("Authorization","Bearer exam-admin").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(get("/admin/api/exam/questions/detail").param("questionId","0").header("Authorization","Bearer exam-admin")).andExpect(status().isBadRequest());
    }

    @Test void sourceBlueprintAndDetailReadEndpointsReturnTheirData() throws Exception {
        when(query.sources(null,null,null,null,null,1,20)).thenReturn(new com.mall.common.api.PageResponse<>(java.util.Collections.emptyList(),1,20,3));
        mvc.perform(get("/admin/api/exam/sources").header("Authorization","Bearer exam-admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(3));
        com.mall.exam.admin.dto.AdminExamResponses.Blueprint blueprint=new com.mall.exam.admin.dto.AdminExamResponses.Blueprint();
        blueprint.id=1L;blueprint.completePaperCapacity=5;
        when(query.blueprints()).thenReturn(java.util.Collections.singletonList(blueprint));
        mvc.perform(get("/admin/api/exam/blueprints").header("Authorization","Bearer exam-admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].completePaperCapacity").value(5));
        com.mall.exam.question.model.QuestionEntity question=new com.mall.exam.question.model.QuestionEntity();
        question.setId(8L);question.setCorrectAnswer("[\"A\"]");
        when(query.detail(8L)).thenReturn(new com.mall.exam.admin.dto.AdminExamResponses.QuestionDetail(null,java.util.Collections.singletonList(question)));
        mvc.perform(get("/admin/api/exam/questions/detail").param("questionId","8").header("Authorization","Bearer exam-admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.questions[0].correctAnswer").value("[\"A\"]"));
    }

    @Test void recordListExposesOnlyPermittedRecordMetadata() throws Exception {
        com.mall.exam.admin.dto.AdminExamResponses.Record record=new com.mall.exam.admin.dto.AdminExamResponses.Record();
        record.id=11L;record.userId=7L;record.paperNo="EX11";record.status="SUBMITTED";
        when(query.records(null,null,null,1,20)).thenReturn(new com.mall.common.api.PageResponse<>(java.util.Collections.singletonList(record),1,20,1));
        mvc.perform(get("/admin/api/exam/records").header("Authorization","Bearer exam-admin"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.records[0].paperNo").value("EX11"))
            .andExpect(jsonPath("$.data.records[0].answerContent").doesNotExist())
            .andExpect(jsonPath("$.data.records[0].correctAnswer").doesNotExist())
            .andExpect(jsonPath("$.data.records[0].referenceAnswer").doesNotExist())
            .andExpect(jsonPath("$.data.records[0].sampleAnswer").doesNotExist());
    }

    @Test void multipartSanitizesFilenameAndPropagatesBytesAndPrincipal() throws Exception {
        byte[] bytes="[]".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        mvc.perform(multipart("/admin/api/exam/imports/preview").file(new org.springframework.mock.web.MockMultipartFile("file","../bank.json","application/json",bytes))
            .param("sourceId","9").param("adminId","999").header("Authorization","Bearer exam-admin")).andExpect(status().isOk());
        org.mockito.Mockito.verify(imports).preview(9L,"bank.json",bytes,42L);
    }

    @Test void multipartRejectsMissingEmptyOversizedAndUnsupportedFiles() throws Exception {
        mvc.perform(multipart("/admin/api/exam/imports/preview").param("sourceId","9").header("Authorization","Bearer exam-admin")).andExpect(status().isBadRequest());
        for(org.springframework.mock.web.MockMultipartFile file: new org.springframework.mock.web.MockMultipartFile[]{
            new org.springframework.mock.web.MockMultipartFile("file","a.json","application/json",new byte[0]),
            new org.springframework.mock.web.MockMultipartFile("file","a.json","application/json",new byte[5*1024*1024+1]),
            new org.springframework.mock.web.MockMultipartFile("file","a.exe","application/octet-stream",new byte[]{1})})
            mvc.perform(multipart("/admin/api/exam/imports/preview").file(file).param("sourceId","9").header("Authorization","Bearer exam-admin"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        mvc.perform(multipart("/admin/api/exam/imports/preview").file(new org.springframework.mock.web.MockMultipartFile("file","a.json","application/json",new byte[]{1}))
            .param("sourceId","0").header("Authorization","Bearer exam-admin")).andExpect(status().isBadRequest());
        org.mockito.Mockito.verifyNoInteractions(imports);
    }
}

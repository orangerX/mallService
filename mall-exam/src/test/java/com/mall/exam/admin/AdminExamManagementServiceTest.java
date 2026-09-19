package com.mall.exam.admin;

import com.mall.exam.admin.service.AdminExamManagementService;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.question.mapper.*;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.question.service.QuestionGovernanceService;
import com.mall.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminExamManagementServiceTest {
    QuestionSourceMapper sources=mock(QuestionSourceMapper.class);
    QuestionMapper questions=mock(QuestionMapper.class);
    AdminExamMapper admin=mock(AdminExamMapper.class);
    com.mall.exam.importer.mapper.ImportBatchMapper reservations=mock(com.mall.exam.importer.mapper.ImportBatchMapper.class);
    AdminExamManagementService service=new AdminExamManagementService(sources,questions,admin,new QuestionGovernanceService(sources,questions),reservations);
    @org.junit.jupiter.api.BeforeEach void reservations() {
        when(reservations.reserveFingerprint(org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.isNull())).thenReturn(1);
        when(admin.questionForUpdate(org.mockito.ArgumentMatchers.anyLong())).thenAnswer(call->questions.findById(call.getArgument(0)));
    }

    @Test void cannotRewriteAnApprovedSource() {
        QuestionSourceEntity source=new QuestionSourceEntity(); source.setId(9L); source.setReviewStatus("APPROVED");
        when(sources.findByIdForUpdate(9L)).thenReturn(source);
        SourceUpdate body=new SourceUpdate(); body.sourceId=9L;
        assertThrows(BusinessException.class,()->service.updateSource(body,42L));
        verify(sources,never()).update(any());
    }
    @Test void disableSourcePreservesApprovalAndUsesSeparateEnabledField() {
        when(admin.sourceStatus(9L,0)).thenReturn(1);
        service.sourceStatus(9L,0,42L);
        verify(admin).sourceStatus(9L,0);
        verifyNoInteractions(sources);
    }
    @Test void invalidGroupCannotCreateAnyRows() {
        QuestionSourceEntity source=new QuestionSourceEntity(); source.setId(9L);
        when(sources.findByIdForUpdate(9L)).thenReturn(source);
        Question body=new Question(); body.sourceId=9L; body.content=new Content();
        body.content.groupType="READING"; body.content.title="Passage"; body.content.content="Text";
        body.content.questions=java.util.Collections.emptyList();
        assertThrows(BusinessException.class,()->service.createQuestion(body,42L));
        verify(questions,never()).insert(any()); verify(questions,never()).insertGroup(any());
    }

    @Test void createsAnEntireTwoBlankDialogueAsOneDraftGroup() throws Exception {
        QuestionSourceEntity source=new QuestionSourceEntity();source.setId(9L);
        when(sources.findByIdForUpdate(9L)).thenReturn(source);
        when(questions.insertGroup(any())).thenAnswer(call->{call.getArgument(0,com.mall.exam.question.model.QuestionGroupEntity.class).setId(7L);return 1;});
        java.util.List<com.mall.exam.question.model.QuestionEntity> saved=new java.util.ArrayList<>();
        when(questions.insert(any())).thenAnswer(call->{
            com.mall.exam.question.model.QuestionEntity item=call.getArgument(0); item.setId(20L+saved.size());saved.add(item);return 1;
        });
        Question body=new Question();body.sourceId=9L;
        body.content=new Content();body.content.groupType="DIALOGUE";body.content.title="Greeting";body.content.content="A dialogue";
        body.content.questions=java.util.Arrays.asList(validQuestion("First blank","DIALOGUE_BLANK"),validQuestion("Second blank","DIALOGUE_BLANK"));
        assertEquals(20L,service.createQuestion(body,42L));
        assertEquals(2,saved.size());assertEquals(7L,saved.get(0).getGroupId());assertEquals(7L,saved.get(1).getGroupId());
        assertEquals(1,saved.get(0).getGroupSortOrder());assertEquals(2,saved.get(1).getGroupSortOrder());
        assertEquals("DRAFT",saved.get(0).getReviewStatus());assertEquals("[\"A\"]",saved.get(0).getCorrectAnswer());
    }

    @Test void publishedQuestionCannotBeEdited() throws Exception {
        QuestionSourceEntity source=new QuestionSourceEntity();source.setId(9L);
        when(sources.findByIdForUpdate(9L)).thenReturn(source);
        com.mall.exam.question.model.QuestionEntity original=new com.mall.exam.question.model.QuestionEntity();
        original.setId(8L);original.setReviewStatus("APPROVED");when(questions.findById(8L)).thenReturn(original);
        QuestionUpdate body=new QuestionUpdate();body.sourceId=9L;body.questionId=8L;body.content=validQuestion("New stem","GRAMMAR");
        assertThrows(BusinessException.class,()->service.updateQuestion(body,42L));
        verify(admin,never()).updateDraftQuestion(any());
    }

    @Test void guardedDraftUpdateConflictsInsteadOfSilentlySucceeding() throws Exception {
        QuestionSourceEntity source=new QuestionSourceEntity();source.setId(9L);
        when(sources.findByIdForUpdate(9L)).thenReturn(source);
        com.mall.exam.question.model.QuestionEntity original=new com.mall.exam.question.model.QuestionEntity();
        original.setId(8L);original.setReviewStatus("DRAFT");when(questions.findById(8L)).thenReturn(original);
        QuestionUpdate body=new QuestionUpdate();body.sourceId=9L;body.questionId=8L;body.content=validQuestion("New stem","GRAMMAR");
        BusinessException error=assertThrows(BusinessException.class,()->service.updateQuestion(body,42L));
        assertEquals(com.mall.common.api.ErrorCode.DATA_CONFLICT,error.getErrorCode());
        org.mockito.ArgumentCaptor<com.mall.exam.question.model.QuestionEntity> item=org.mockito.ArgumentCaptor.forClass(com.mall.exam.question.model.QuestionEntity.class);
        verify(admin).updateDraftQuestion(item.capture());assertEquals(8L,item.getValue().getId());
    }

    private Content validQuestion(String stem,String type) throws Exception {
        Content content=new Content();content.questionType=type;content.stem=stem;content.difficulty=2;content.explanation="Explanation";
        com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper();
        content.options=json.readTree("{\"A\":\"One\",\"B\":\"Two\",\"C\":\"Three\",\"D\":\"Four\"}");
        content.correctAnswer=json.readTree("\"A\"");return content;
    }
}

package com.mall.exam.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.exception.BusinessException;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.admin.service.AdminExamManagementService;
import com.mall.exam.importer.QuestionFingerprint;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import com.mall.exam.question.mapper.*;
import com.mall.exam.question.model.*;
import com.mall.exam.question.service.QuestionGovernanceService;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AdminExamGroupReconciliationTest {
    QuestionSourceMapper sources=mock(QuestionSourceMapper.class);
    QuestionMapper questions=mock(QuestionMapper.class);
    AdminExamMapper mapper=mock(AdminExamMapper.class);
    ImportBatchMapper fingerprints=mock(ImportBatchMapper.class);
    AdminExamManagementService service=new AdminExamManagementService(sources,questions,mapper,new QuestionGovernanceService(sources,questions),fingerprints);
    List<QuestionEntity> members=new ArrayList<>();
    QuestionGroupEntity group=new QuestionGroupEntity();
    @BeforeEach void setUp() {
        QuestionSourceEntity source=new QuestionSourceEntity();source.setId(9L);when(sources.findByIdForUpdate(9L)).thenReturn(source);
        group.setId(7L);group.setGroupType("DIALOGUE");group.setReviewStatus("DRAFT");group.setContentFingerprint(QuestionFingerprint.sha256("Material","Title"));
        when(questions.findGroupByIdForUpdate(7L)).thenReturn(group);
        when(questions.findByGroupIdForUpdate(7L)).thenAnswer(call->new ArrayList<>(members));
        when(fingerprints.reserveFingerprint(anyString(),anyString(),isNull())).thenReturn(1);
        when(mapper.updateDraftGroup(any())).thenReturn(1);when(mapper.updateDraftQuestion(any())).thenReturn(1);
        when(mapper.deleteDraftMember(anyLong(),eq(7L))).thenReturn(1);
        when(questions.insert(any())).thenAnswer(call->{call.getArgument(0,QuestionEntity.class).setId(100L);return 1;});
        seed(2);
    }
    void seed(int count) {
        members.clear();
        for(int i=0;i<count;i++){QuestionEntity q=new QuestionEntity();q.setId(20L+i);q.setGroupId(7L);q.setGroupSortOrder(i+1);q.setReviewStatus("DRAFT");q.setContentFingerprint(QuestionFingerprint.sha256("Material","Q"+i));members.add(q);}
        when(questions.findById(20L)).thenReturn(members.get(0));
    }
    @Test void reordersRetainedMembersByIdInsteadOfPosition() throws Exception {
        service.updateQuestion(request("DIALOGUE",member(21L,"Second","DIALOGUE_BLANK"),member(20L,"First","DIALOGUE_BLANK")),42L);
        ArgumentCaptor<QuestionEntity> saved=ArgumentCaptor.forClass(QuestionEntity.class);
        verify(mapper,times(2)).updateDraftQuestion(saved.capture());
        assertEquals(21L,saved.getAllValues().get(0).getId());assertEquals(1,saved.getAllValues().get(0).getGroupSortOrder());
        assertEquals(20L,saved.getAllValues().get(1).getId());assertEquals(2,saved.getAllValues().get(1).getGroupSortOrder());
        verify(questions,never()).insert(any());verify(mapper,never()).deleteDraftMember(anyLong(),anyLong());
    }
    @Test void removesOmittedMembersAndAddsNewDialogueMembers() throws Exception {
        service.updateQuestion(request("DIALOGUE",member(21L,"Retained","DIALOGUE_BLANK"),member(null,"New","DIALOGUE_BLANK"),member(null,"Another","DIALOGUE_BLANK")),42L);
        verify(mapper).deleteDraftMember(20L,7L);verify(questions,times(2)).insert(any());verify(mapper,times(1)).updateDraftQuestion(any());
    }
    @Test void repairsIncompleteFourMemberReadingGroupToFive() throws Exception {
        seed(4);group.setGroupType("READING");
        service.updateQuestion(request("READING",member(20L,"A","READING"),member(21L,"B","READING"),member(22L,"C","READING"),member(23L,"D","READING"),member(null,"E","READING")),42L);
        verify(questions).insert(any());verify(mapper,times(4)).updateDraftQuestion(any());
    }
    @Test void rejectsForeignOrDuplicateMemberIdsBeforeAnyMutation() throws Exception {
        for(Content[] invalid:new Content[][]{
            {member(999L,"Foreign","DIALOGUE_BLANK"),member(21L,"Existing","DIALOGUE_BLANK")},
            {member(20L,"First","DIALOGUE_BLANK"),member(20L,"Second","DIALOGUE_BLANK")}})
            assertThrows(BusinessException.class,()->service.updateQuestion(request("DIALOGUE",invalid),42L));
        verifyNoInteractions(fingerprints);verify(mapper,never()).updateDraftGroup(any());
    }
    @Test void referencedRemovalAndPublishedMemberRejectWholeEditBeforeAnyMutation() throws Exception {
        when(questions.countPaperItemSnapshots(20L)).thenReturn(1L);
        assertThrows(BusinessException.class,()->service.updateQuestion(request("DIALOGUE",member(21L,"Retained","DIALOGUE_BLANK")),42L));
        when(questions.countPaperItemSnapshots(20L)).thenReturn(0L);members.get(0).setReviewStatus("APPROVED");
        assertThrows(BusinessException.class,()->service.updateQuestion(request("DIALOGUE",member(21L,"Retained","DIALOGUE_BLANK")),42L));
        verifyNoInteractions(fingerprints);verify(mapper,never()).updateDraftGroup(any());
    }
    @Test void suppliedIdsAreRejectedOnCreate() throws Exception {
        Question request=request("DIALOGUE",member(20L,"Bad","DIALOGUE_BLANK"));
        assertThrows(BusinessException.class,()->service.createQuestion(request,42L));
        verifyNoInteractions(fingerprints);verify(questions,never()).insert(any());
    }
    QuestionUpdate request(String type,Content... content) {
        QuestionUpdate request=new QuestionUpdate();request.questionId=20L;request.sourceId=9L;
        request.content=new Content();request.content.groupType=type;request.content.title="Title";request.content.content="Material";request.content.questions=Arrays.asList(content);return request;
    }
    Content member(Long id,String stem,String type) throws Exception {
        Content result=new Content();result.questionId=id;result.stem=stem;result.questionType=type;result.difficulty=2;result.explanation="Explanation";
        ObjectMapper json=new ObjectMapper();result.options=json.readTree("{\"A\":\"a\",\"B\":\"b\",\"C\":\"c\",\"D\":\"d\"}");result.correctAnswer=json.readTree("\"A\"");return result;
    }
}

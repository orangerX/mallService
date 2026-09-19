package com.mall.exam.paper.service;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.paper.dto.ExamPaperResponse;
import com.mall.exam.paper.lock.ExamGenerationLock;
import com.mall.exam.paper.mapper.ExamPaperMapper;
import com.mall.exam.paper.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamPaperServiceTest {
    ExamPaperMapper mapper;ExamGenerationLock lock;PlatformTransactionManager transactions;ExamPaperService service;
    final Clock clock=Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void setup() {
        mapper=mock(ExamPaperMapper.class);lock=mock(ExamGenerationLock.class);transactions=mock(PlatformTransactionManager.class);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service=new ExamPaperService(mapper,new ExamPaperAssembler(new Random(3)),lock,transactions,clock);
        when(lock.acquire("mall:exam:generate:user:7",Duration.ofSeconds(10))).thenReturn("owner-token");
        when(mapper.lockUser(7L)).thenReturn(7L);
        when(mapper.findGroupSnapshots(anyLong())).thenReturn(List.of());when(mapper.findItemSnapshots(anyLong())).thenReturn(List.of());
    }
    @Test void resumesExistingPaperWithoutLockOrInsertsAndCurrentChecksOwnership() {
        when(mapper.findInProgress(7)).thenReturn(existing());
        assertEquals(90L,service.generateOrResume(7).id);
        verifyNoInteractions(lock,transactions);verify(mapper,never()).insertPaper(any());
        when(mapper.findInProgress(7)).thenReturn(null);assertNull(service.current(7));
        ExamPaperEntity other=existing();other.setUserId(8L);when(mapper.findInProgress(7)).thenReturn(other);
        assertEquals(ErrorCode.EXAM_NOT_FOUND,assertThrows(BusinessException.class,()->service.current(7)).getErrorCode());
        other.setUserId(7L);other.setStatus("SUBMITTED");
        assertEquals(ErrorCode.EXAM_NOT_FOUND,assertThrows(BusinessException.class,()->service.current(7)).getErrorCode());
    }
    @Test void rechecksAfterAcquiringLockAndCommitsBeforeReleasing() {
        when(mapper.findInProgress(7)).thenReturn(null,existing());
        assertEquals(90L,service.generateOrResume(7).id);
        org.mockito.InOrder order=inOrder(lock,mapper,transactions);
        order.verify(lock).acquire("mall:exam:generate:user:7",Duration.ofSeconds(10));
        order.verify(mapper).lockUser(7);order.verify(mapper).findInProgress(7);
        order.verify(transactions).commit(any());order.verify(lock).release("mall:exam:generate:user:7","owner-token");
        verify(mapper,never()).insertPaper(any());
    }
    @Test void persistsAllImmutableContentWithGeneratedIdsAndStableOrder() throws Exception {
        preparePool();List<ExamPaperGroupSnapshotEntity> groups=new ArrayList<>();List<ExamPaperItemSnapshotEntity> items=new ArrayList<>();
        doAnswer(a->{ExamPaperGroupSnapshotEntity g=a.getArgument(0);g.setId(200L+groups.size());groups.add(g);return 1;}).when(mapper).insertGroupSnapshot(any());
        doAnswer(a->{ExamPaperItemSnapshotEntity q=a.getArgument(0);q.setId(300L+items.size());items.add(q);return 1;}).when(mapper).insertItemSnapshot(any());
        when(mapper.findGroupSnapshots(90)).thenReturn(groups);when(mapper.findItemSnapshots(90)).thenReturn(items);
        ExamPaperResponse response=service.generateOrResume(7);
        assertEquals(7,groups.size());assertEquals(52,items.size());assertEquals(90L,response.id);
        ArgumentCaptor<ExamPaperEntity> paper=ArgumentCaptor.forClass(ExamPaperEntity.class);verify(mapper).insertPaper(paper.capture());
        assertEquals(LocalDateTime.of(2026,9,18,12,0),paper.getValue().getDueAt());
        assertTrue(paper.getValue().getPaperNo().matches("DE-[A-F0-9]{32}"));
        assertEquals("v2",paper.getValue().getBlueprintVersionSnapshot());assertEquals(4L,paper.getValue().getBlueprintId());
        Map<Integer,Integer> orders=new HashMap<>();
        for(ExamPaperItemSnapshotEntity q:items) {
            assertEquals(90L,q.getPaperId());assertNotNull(q.getId());
            int expected=orders.merge(q.getSectionOrder(),1,Integer::sum);assertEquals(expected,q.getItemOrder());
            assertEquals("secret explanation",q.getExplanation());assertEquals("secret reference",q.getReferenceAnswer());
            assertEquals("secret sample",q.getSampleAnswer());assertEquals("\"A\"",q.getCorrectAnswer());
            assertEquals("{\"points\":15}",q.getScoringRubric());assertEquals("[\"tense\"]",q.getKnowledgePoints());
            if(q.getSectionOrder()<3)assertTrue(groups.stream().anyMatch(g->g.getId().equals(q.getPaperGroupSnapshotId())));
            else assertNull(q.getPaperGroupSnapshotId());
        }
        assertEquals(100,items.stream().map(ExamPaperItemSnapshotEntity::getScore).reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add).intValueExact());
        assertEquals("Choose",groups.get(0).getInstruction());assertNotNull(groups.get(0).getContent());assertNotNull(groups.get(0).getSharedOptions());
        String json=new ObjectMapper().findAndRegisterModules().writeValueAsString(response);
        for(String forbidden:List.of("correctAnswer","referenceAnswer","sampleAnswer","explanation","scoringRubric","secret"))assertFalse(json.contains(forbidden),json);
        verify(transactions).commit(any());verify(lock).release("mall:exam:generate:user:7","owner-token");
    }
    @Test void insertionFailureRequestsRollbackAndAlwaysReleasesOwnedLock() {
        preparePool();doThrow(new IllegalStateException("storage failed")).when(mapper).insertItemSnapshot(any());
        assertThrows(IllegalStateException.class,()->service.generateOrResume(7));
        org.mockito.InOrder order=inOrder(transactions,lock);order.verify(transactions).rollback(any());order.verify(lock).release("mall:exam:generate:user:7","owner-token");
        verify(transactions,never()).commit(any());
    }
    @Test void insufficientPoolNeverInsertsPartialPaper() {
        preparePool();when(mapper.findAvailableQuestions()).thenReturn(List.of());
        assertEquals(ErrorCode.EXAM_BANK_INSUFFICIENT,assertThrows(BusinessException.class,()->service.generateOrResume(7)).getErrorCode());
        verify(mapper,never()).insertPaper(any());verify(transactions).rollback(any());
    }
    @Test void lockFailureOrTimeoutNeverGeneratesOrReleasesUnownedLock() {
        when(lock.acquire(anyString(),any())).thenReturn(null);
        assertEquals(ErrorCode.DATA_CONFLICT,assertThrows(BusinessException.class,()->service.generateOrResume(7)).getErrorCode());
        verifyNoInteractions(transactions);verify(lock,never()).release(anyString(),anyString());verify(mapper,never()).insertPaper(any());
        when(lock.acquire(anyString(),any())).thenThrow(new IllegalStateException("Redis unavailable"));
        assertThrows(IllegalStateException.class,()->service.generateOrResume(7));verifyNoInteractions(transactions);
    }
    @Test void aSecondGenerationRequestReturnsCommittedPaper() {
        preparePool();service.generateOrResume(7);when(mapper.findInProgress(7)).thenReturn(existing());
        service.generateOrResume(7);verify(mapper,times(1)).insertPaper(any());verify(lock,times(1)).acquire(anyString(),any());
    }
    private void preparePool() {
        QuestionPool p=ExamPaperAssemblerTest.pool();List<ExamPaperMapper.AvailableGroup> available=new ArrayList<>();
        for(com.mall.exam.question.model.QuestionGroupEntity group:p.getGroups()) {
            ExamPaperMapper.AvailableGroup row=new ExamPaperMapper.AvailableGroup();
            org.springframework.beans.BeanUtils.copyProperties(group,row);row.setMemberCount(p.getMemberCounts().get(group.getId()));available.add(row);
        }
        when(mapper.findAvailableGroups()).thenReturn(available);when(mapper.findAvailableQuestions()).thenReturn(p.getQuestions());
        when(mapper.findPreviouslyUsedQuestionIds(7)).thenReturn(List.of());when(mapper.findPreviouslyUsedGroupIds(7)).thenReturn(List.of());
        ExamPaperMapper.BlueprintRow b=new ExamPaperMapper.BlueprintRow();b.id=4L;b.version="v2";when(mapper.findEnabledBlueprint(anyString())).thenReturn(b);
        doAnswer(a->{ExamPaperEntity paper=a.getArgument(0);paper.setId(90L);return 1;}).when(mapper).insertPaper(any());
        doAnswer(a->{ExamPaperGroupSnapshotEntity g=a.getArgument(0);g.setId(g.getOriginalGroupId()+2000);return 1;}).when(mapper).insertGroupSnapshot(any());
        doAnswer(a->{ExamPaperItemSnapshotEntity q=a.getArgument(0);q.setId(q.getOriginalQuestionId()+3000);return 1;}).when(mapper).insertItemSnapshot(any());
    }
    private ExamPaperEntity existing() {
        ExamPaperEntity p=new ExamPaperEntity();p.setId(90L);p.setUserId(7L);p.setStatus("IN_PROGRESS");p.setPaperNo("old");
        p.setStartedAt(LocalDateTime.of(2026,9,18,10,0));p.setDueAt(LocalDateTime.of(2026,9,18,12,0));return p;
    }
}

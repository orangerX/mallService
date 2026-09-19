package com.mall.exam.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.admin.dto.AdminExamRequests.*;
import com.mall.exam.admin.service.AdminExamManagementService;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import com.mall.exam.importer.model.*;
import com.mall.exam.question.mapper.*;
import com.mall.exam.question.model.*;
import com.mall.exam.question.service.QuestionGovernanceService;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.junit.jupiter.api.*;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** Real services/coordinator + Spring transaction advice over an observable in-memory mapper boundary. */
class ManualImportFingerprintIntegrationTest {
    final QuestionSourceMapper sources=mock(QuestionSourceMapper.class);
    final QuestionMapper questions=mock(QuestionMapper.class);
    final AdminExamMapper admin=mock(AdminExamMapper.class);
    final ImportBatchMapper imports=mock(ImportBatchMapper.class);
    final Map<Long,QuestionEntity> rows=new LinkedHashMap<>();
    final Map<Long,QuestionGroupEntity> groups=new LinkedHashMap<>();
    final Map<String,Long> reservations=new HashMap<>();
    final Map<Long,ImportBatchEntity> batches=new HashMap<>();
    final Map<Long,ImportBatchItemEntity> importedItems=new LinkedHashMap<>();
    final ObjectMapper json=new ObjectMapper().setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
    long sequence=100;
    int failAfterInserts=-1;
    AdminExamManagementService management;
    ExamImportService importer;

    @BeforeEach void setUp() {
        QuestionSourceEntity source=new QuestionSourceEntity();source.setId(9L);source.setCopyrightStatus("ORIGINAL");source.setReviewStatus("APPROVED");
        when(sources.findById(9L)).thenReturn(source);when(sources.findByIdForUpdate(9L)).thenReturn(source);
        when(questions.findById(anyLong())).thenAnswer(c->rows.get(c.getArgument(0)));
        when(admin.questionForUpdate(anyLong())).thenAnswer(c->rows.get(c.getArgument(0)));
        when(questions.findGroupByIdForUpdate(anyLong())).thenAnswer(c->groups.get(c.getArgument(0)));
        when(questions.findByGroupIdForUpdate(anyLong())).thenAnswer(c->rows.values().stream().filter(q->Objects.equals(q.getGroupId(),c.getArgument(0))).collect(Collectors.toList()));
        when(questions.insert(any())).thenAnswer(c->{
            if(failAfterInserts==0) throw conflict();
            if(failAfterInserts>0)failAfterInserts--;
            QuestionEntity q=c.getArgument(0);q.setId(++sequence);rows.put(q.getId(),q);return 1;
        });
        when(questions.insertGroup(any())).thenAnswer(c->{QuestionGroupEntity g=c.getArgument(0);g.setId(++sequence);groups.put(g.getId(),g);return 1;});
        when(admin.updateDraftQuestion(any())).thenAnswer(c->{QuestionEntity q=c.getArgument(0);if(!rows.containsKey(q.getId()))return 0;rows.put(q.getId(),q);return 1;});
        when(admin.updateDraftGroup(any())).thenAnswer(c->{QuestionGroupEntity g=c.getArgument(0);if(!groups.containsKey(g.getId()))return 0;groups.put(g.getId(),g);return 1;});
        when(admin.deleteDraftMember(anyLong(),anyLong())).thenAnswer(c->{QuestionEntity q=rows.get(c.getArgument(0));return q!=null&&Objects.equals(q.getGroupId(),c.getArgument(1))&&rows.remove(q.getId())!=null?1:0;});
        when(questions.countQuestionsByContentFingerprint(anyString())).thenAnswer(c->(long)questionIds(c.getArgument(0)).size());
        when(questions.countGroupsByContentFingerprint(anyString())).thenAnswer(c->(long)groupIds(c.getArgument(0)).size());
        when(imports.fingerprintQuestionIds(anyString())).thenAnswer(c->questionIds(c.getArgument(0)));
        when(imports.fingerprintGroupIds(anyString())).thenAnswer(c->groupIds(c.getArgument(0)));
        when(imports.reserveFingerprint(anyString(),anyString(),nullable(Long.class))).thenAnswer(c->{
            String key=c.getArgument(0);if(reservations.containsKey(key))return 0;reservations.put(key,c.getArgument(2));return 1;
        });
        when(imports.lockFingerprint(anyString())).thenAnswer(c->reservations.containsKey(c.getArgument(0))?c.getArgument(0):null);
        when(imports.releaseUnusedFingerprint(anyString())).thenAnswer(c->{
            String key=c.getArgument(0);Long owner=reservations.get(key);ImportBatchEntity batch=batches.get(owner);
            boolean active=batch!=null&&Arrays.asList("UPLOADED","VALIDATED").contains(batch.getStatus());
            if(!reservations.containsKey(key)||active||!questionIds(key).isEmpty()||!groupIds(key).isEmpty())return 0;
            reservations.remove(key);return 1;
        });
        when(imports.releaseFingerprint(anyString(),anyLong())).thenAnswer(c->{
            String key=c.getArgument(0);if(!reservations.containsKey(key)||!Objects.equals(reservations.get(key),c.getArgument(1)))return 0;
            reservations.remove(key);return 1;
        });
        when(imports.insertBatch(any())).thenAnswer(c->{ImportBatchEntity b=c.getArgument(0);b.setId(++sequence);batches.put(b.getId(),b);return 1;});
        when(imports.insertItem(any())).thenAnswer(c->{ImportBatchItemEntity i=c.getArgument(0);i.setId(++sequence);importedItems.put(i.getId(),i);return 1;});
        when(imports.findBatchByIdForUpdate(anyLong())).thenAnswer(c->batches.get(c.getArgument(0)));
        when(imports.findItemsByBatchIdForUpdate(anyLong())).thenAnswer(c->importedItems.values().stream().filter(i->Objects.equals(i.getBatchId(),c.getArgument(0))).collect(Collectors.toList()));
        when(imports.markItemImported(anyLong())).thenAnswer(c->{importedItems.get(c.getArgument(0)).setStatus("IMPORTED");return 1;});
        when(imports.markItemDuplicate(anyLong())).thenAnswer(c->{importedItems.get(c.getArgument(0)).setStatus("DUPLICATE");return 1;});
        when(imports.markImported(anyLong(),anyLong(),anyInt(),anyInt(),anyInt(),anyString())).thenAnswer(c->{
            ImportBatchEntity b=batches.get(c.getArgument(0));Long actor=c.getArgument(1);
            if(!"VALIDATED".equals(b.getStatus())||(b.getImportedBy()!=null&&!b.getImportedBy().equals(actor)))return 0;
            b.setStatus("IMPORTED");if(b.getImportedBy()==null)b.setImportedBy(actor);b.setSuccessRows(c.getArgument(2));b.setDuplicateRows(c.getArgument(4));return 1;
        });
        Tx tx=new Tx();
        management=proxy(new AdminExamManagementService(sources,questions,admin,new QuestionGovernanceService(sources,questions),imports),tx);
        importer=proxy(new ExamImportService(sources,questions,imports),tx);
    }

    @Test void importedAEditedToBReleasesAAndAllowsReimport() throws Exception {
        long batch=preview("A").getBatchId();importer.commit(batch,42L);
        Long id=rows.keySet().iterator().next();
        QuestionUpdate edit=new QuestionUpdate();edit.sourceId=9L;edit.questionId=id;edit.content=content("B","GRAMMAR");
        management.updateQuestion(edit,42L);
        assertFalse(reservations.containsKey(QuestionFingerprint.sha256("","A")));
        importer.commit(preview("A").getBatchId(),42L);
        assertEquals(new HashSet<>(Arrays.asList("A","B")),rows.values().stream().map(QuestionEntity::getStem).collect(Collectors.toSet()));
        assertEquals(2,rows.size());assertEquals(2,reservations.size());
    }

    @Test void manualCreateAfterPreviewMakesImportSkipTheNowExistingQuestion() throws Exception {
        long batch=preview("A").getBatchId();
        management.createQuestion(single("A"),42L);
        importer.commit(batch,42L);
        assertEquals(1,rows.size());assertEquals(0,batches.get(batch).getSuccessRows());
        assertEquals(1,batches.get(batch).getDuplicateRows());
        assertNull(reservations.get(QuestionFingerprint.sha256("","A")));
    }

    @Test void editingOneLegacyDuplicateDoesNotReleaseTheRemainingRowsFingerprint() throws Exception {
        importer.commit(preview("A").getBatchId(),42L);
        QuestionEntity original=rows.values().iterator().next();
        QuestionEntity duplicate=json.convertValue(original,QuestionEntity.class);duplicate.setId(++sequence);rows.put(duplicate.getId(),duplicate);
        QuestionUpdate edit=new QuestionUpdate();edit.sourceId=9L;edit.questionId=original.getId();edit.content=content("B","GRAMMAR");
        management.updateQuestion(edit,42L);
        assertTrue(reservations.containsKey(QuestionFingerprint.sha256("","A")));
        assertEquals(1,questionIds(QuestionFingerprint.sha256("","A")).size());
    }

    @Test void staleTerminalReservationCanBeReusedButActiveReservationCannotBeDeleted() throws Exception {
        ImportBatchEntity stale=new ImportBatchEntity();stale.setId(5L);stale.setStatus("IMPORTED");batches.put(5L,stale);
        String a=QuestionFingerprint.sha256("","A");reservations.put(a,5L);
        management.createQuestion(single("A"),42L);assertNull(reservations.get(a));
        ImportBatchEntity active=new ImportBatchEntity();active.setId(6L);active.setStatus("VALIDATED");batches.put(6L,active);
        String b=QuestionFingerprint.sha256("","B");reservations.put(b,6L);
        assertThrows(BusinessException.class,()->management.createQuestion(single("B"),42L));
        assertEquals(6L,reservations.get(b));assertEquals(1,rows.size());
    }

    @Test void actualLegacyRowsWithoutReservationsStillPreventDuplicateManualCreation() throws Exception {
        management.createQuestion(single("A"),42L);reservations.clear();
        assertThrows(BusinessException.class,()->management.createQuestion(single("A"),42L));
        assertEquals(1,rows.size());assertTrue(reservations.isEmpty());
    }

    @Test void failedManualCreateRollsBackOnlyItsNewRowsAndReservations() throws Exception {
        management.createQuestion(single("Existing"),42L);
        Map<String,Long> before=new HashMap<>(reservations);
        failAfterInserts=1;
        assertThrows(BusinessException.class,()->management.createQuestion(dialogue("A","B"),42L));
        assertEquals(before,reservations);assertEquals(1,rows.size());assertTrue(groups.isEmpty());
    }

    @Test void failedGroupEditRestoresRetainedIdsOrderRemovedMembersAndOldFingerprints() throws Exception {
        long anchor=management.createQuestion(dialogue("A","B"),42L);
        List<QuestionEntity> old=new ArrayList<>(rows.values());Map<String,Long> before=new HashMap<>(reservations);
        QuestionUpdate edit=new QuestionUpdate();edit.sourceId=9L;edit.questionId=anchor;
        edit.content=dialogue("Changed","New").content;edit.content.content="Changed material";
        edit.content.questions.get(0).questionId=old.get(1).getId();
        failAfterInserts=0;
        assertThrows(BusinessException.class,()->management.updateQuestion(edit,42L));
        assertEquals(before,reservations);assertEquals(2,rows.size());
        assertEquals(old.get(0).getStem(),rows.get(old.get(0).getId()).getStem());
        assertEquals(old.get(1).getGroupSortOrder(),rows.get(old.get(1).getId()).getGroupSortOrder());
        assertEquals("Material",groups.values().iterator().next().getContent());
    }

    ImportPreviewResponse preview(String stem) throws Exception {
        ImportPreviewResponse result=importer.preview(9L,"questions.json",json.writeValueAsBytes(content(stem,"GRAMMAR")),42L);
        assertEquals("VALIDATED",batches.get(result.getBatchId()).getStatus(),batches.get(result.getBatchId()).getStructureErrors());
        return result;
    }
    Question single(String stem) throws Exception { Question q=new Question();q.sourceId=9L;q.content=content(stem,"GRAMMAR");return q; }
    Question dialogue(String... stems) throws Exception {
        Question q=new Question();q.sourceId=9L;q.content=new Content();q.content.groupType="DIALOGUE";q.content.title="Title";q.content.content="Material";q.content.questions=new ArrayList<>();
        for(String stem:stems)q.content.questions.add(content(stem,"DIALOGUE_BLANK"));return q;
    }
    Content content(String stem,String type) throws Exception {
        Content c=new Content();c.stem=stem;c.questionType=type;c.difficulty=2;c.explanation="Explanation";
        c.options=json.readTree("{\"A\":\"a\",\"B\":\"b\",\"C\":\"c\",\"D\":\"d\"}");c.correctAnswer=json.readTree("\"A\"");return c;
    }
    List<Long> questionIds(String key){return rows.values().stream().filter(q->key.equals(q.getContentFingerprint())).map(QuestionEntity::getId).collect(Collectors.toList());}
    List<Long> groupIds(String key){return groups.values().stream().filter(g->key.equals(g.getContentFingerprint())).map(QuestionGroupEntity::getId).collect(Collectors.toList());}
    static BusinessException conflict(){return new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT);}
    @SuppressWarnings("unchecked") <T> T proxy(T target,Tx tx) {
        ProxyFactory factory=new ProxyFactory(target);factory.setProxyTargetClass(true);
        factory.addAdvice(new TransactionInterceptor(tx,new AnnotationTransactionAttributeSource()));
        return (T)factory.getProxy();
    }
    final class Snapshot {
        Map<Long,QuestionEntity> questions=new LinkedHashMap<>(rows);
        Map<Long,QuestionGroupEntity> questionGroups=new LinkedHashMap<>(groups);
        Map<String,Long> hashes=new HashMap<>(reservations);
    }
    final class Tx extends AbstractPlatformTransactionManager {
        protected Object doGetTransaction(){return new Object[]{null};}
        protected void doBegin(Object transaction,TransactionDefinition definition){((Object[])transaction)[0]=new Snapshot();}
        protected void doCommit(DefaultTransactionStatus status){}
        protected void doRollback(DefaultTransactionStatus status){
            Snapshot s=(Snapshot)((Object[])status.getTransaction())[0];
            rows.clear();rows.putAll(s.questions);groups.clear();groups.putAll(s.questionGroups);reservations.clear();reservations.putAll(s.hashes);
        }
    }
}

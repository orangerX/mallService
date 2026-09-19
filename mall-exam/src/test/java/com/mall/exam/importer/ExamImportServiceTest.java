package com.mall.exam.importer;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import com.mall.exam.importer.model.ImportBatchEntity;
import com.mall.exam.importer.model.ImportBatchItemEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamImportServiceTest {

    @Mock QuestionSourceMapper sourceMapper;
    @Mock QuestionMapper questionMapper;
    @Mock ImportBatchMapper batchMapper;
    private ExamImportService service;
    // Model the mapper's durable boundary: a new service instance must reload this state.
    private ImportBatchEntity persistedBatch;
    private final List<ImportBatchItemEntity> persistedItems = new ArrayList<>();
    private final Map<String, Long> reservations = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new ExamImportService(sourceMapper, questionMapper, batchMapper);
        lenient().when(batchMapper.insertBatch(any(ImportBatchEntity.class))).thenAnswer(invocation -> {
            persistedBatch = invocation.getArgument(0, ImportBatchEntity.class);
            persistedBatch.setId(101L);
            return 1;
        });
        lenient().when(batchMapper.insertItem(any())).thenAnswer(invocation -> {
            ImportBatchItemEntity item = invocation.getArgument(0);
            item.setId(201L + persistedItems.size());
            persistedItems.add(item);
            return 1;
        });
        lenient().when(batchMapper.findBatchByIdForUpdate(101L)).thenAnswer(invocation -> persistedBatch);
        lenient().when(batchMapper.findItemsByBatchIdForUpdate(101L)).thenAnswer(invocation -> new ArrayList<>(persistedItems));
        lenient().when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source("ORIGINAL", "APPROVED"));
        lenient().when(batchMapper.reserveFingerprint(anyString(), anyString(), anyLong())).thenAnswer(invocation ->
                reservations.putIfAbsent(invocation.getArgument(0), invocation.getArgument(2)) == null ? 1 : 0);
        lenient().when(batchMapper.releaseFingerprint(anyString(), anyLong())).thenAnswer(invocation ->
                reservations.remove(invocation.getArgument(0), invocation.getArgument(1)) ? 1 : 0);
        lenient().when(batchMapper.markItemImported(anyLong())).thenAnswer(invocation -> setItemStatus(invocation.getArgument(0), "IMPORTED"));
        lenient().when(batchMapper.markItemDuplicate(anyLong())).thenAnswer(invocation -> setItemStatus(invocation.getArgument(0), "DUPLICATE"));
        lenient().when(questionMapper.insert(any())).thenReturn(1);
        lenient().when(questionMapper.insertGroup(any())).thenAnswer(invocation -> {
            invocation.getArgument(0, QuestionGroupEntity.class).setId(501L);
            return 1;
        });
        lenient().when(batchMapper.markImported(anyLong(), anyLong(), anyInt(), anyInt(), anyInt(), anyString()))
                .thenAnswer(invocation -> {
                    if (!"VALIDATED".equals(persistedBatch.getStatus())) return 0;
                    persistedBatch.setStatus("IMPORTED");
                    persistedBatch.setImportedBy(invocation.getArgument(1));
                    persistedBatch.setSuccessRows(invocation.getArgument(2));
                    persistedBatch.setFailedRows(invocation.getArgument(3));
                    persistedBatch.setDuplicateRows(invocation.getArgument(4));
                    persistedBatch.setDuplicateWarnings(invocation.getArgument(5));
                    return 1;
                });
    }

    @Test
    void anotherAdminCannotCommitAnOwnedPreviewOrTouchAnyWriteBoundary() {
        when(sourceMapper.findById(9L)).thenReturn(source("ORIGINAL","APPROVED"));
        service.preview(9L,"bank.json",jsonQuestion().getBytes(StandardCharsets.UTF_8),41L);
        clearInvocations(batchMapper,questionMapper,sourceMapper);
        BusinessException error=assertThrows(BusinessException.class,()->service.commit(101L,42L));
        assertEquals(ErrorCode.FORBIDDEN,error.getErrorCode());
        verify(batchMapper).findBatchByIdForUpdate(101L);
        verifyNoMoreInteractions(batchMapper);
        verifyNoInteractions(questionMapper,sourceMapper);
        assertEquals(41L,persistedBatch.getImportedBy());
        service.commit(101L,41L);
        assertEquals("IMPORTED",persistedBatch.getStatus());
        assertEquals(41L,persistedBatch.getImportedBy());
    }

    @Test
    void adminPreviewPersistsAuthenticatedImporterAndRejectsUnknownSource() {
        when(sourceMapper.findById(9L)).thenReturn(source("ORIGINAL","APPROVED"));
        service.preview(9L,"bank.json",jsonQuestion().getBytes(StandardCharsets.UTF_8),42L);
        assertEquals(42L,persistedBatch.getImportedBy());
        assertEquals(9L,persistedBatch.getSourceId());
        assertThrows(BusinessException.class,()->service.preview(999L,"bank.json",new byte[]{1},42L));
    }

    @Test
    void disabledSourceCannotCommitPreviewedQuestions() {
        ImportPreviewResponse preview=service.preview(9L,"bank.json",jsonQuestion().getBytes(StandardCharsets.UTF_8));
        QuestionSourceEntity disabled=source("ORIGINAL","APPROVED"); disabled.setEnabled(0);
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(disabled);
        assertThrows(BusinessException.class,()->service.commit(preview.getBatchId(),42L));
        verify(questionMapper,never()).insert(any());
    }

    @Test
    void previewsJsonAndCsvWithQuotedEmbeddedNewlines() {
        ImportPreviewResponse json = service.preview(9L, "questions.json", jsonQuestion().getBytes(StandardCharsets.UTF_8));
        ImportPreviewResponse csv = service.preview(9L, "questions.csv", csvQuestion().getBytes(StandardCharsets.UTF_8));

        assertEquals(1, json.getTotalRows());
        assertEquals(1, json.getValidRows());
        assertTrue(json.getErrors().isEmpty());
        assertEquals(1, csv.getValidRows());
        assertTrue(csv.getItems().get(0).getStem().contains("line two"));
    }

    @Test
    void previewReportsDeterministicDuplicatesWithoutWritingQuestions() {
        String duplicated = "[" + standaloneQuestion("same stem") + "," + standaloneQuestion("same stem") + "]";

        ImportPreviewResponse preview = service.preview(9L, "duplicates.json", duplicated.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, preview.getTotalRows());
        assertEquals(1, preview.getValidRows());
        assertEquals(1, preview.getDuplicateRows());
        assertTrue(preview.getErrors().isEmpty());
        verify(questionMapper, never()).insert(any(QuestionEntity.class));
        verify(questionMapper, never()).insertGroup(any());
    }

    @Test
    void commitRejectsBatchWithStructuralErrorsBeforeWriting() {
        ImportPreviewResponse preview = service.preview(9L, "invalid.json", "[{\"questionType\":\"VOCABULARY\"}]"
                .getBytes(StandardCharsets.UTF_8));

        BusinessException error = assertThrows(BusinessException.class, () -> service.commit(preview.getBatchId(), 3L));

        assertEquals(ErrorCode.EXAM_IMPORT_INVALID, error.getErrorCode());
    }

    @Test
    void commitRejectsUnapprovedSource() {
        ImportPreviewResponse preview = service.preview(9L, "valid.json", jsonQuestion().getBytes(StandardCharsets.UTF_8));
        when(batchMapper.findBatchByIdForUpdate(101L)).thenReturn(batch("VALIDATED", 9L));
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source("UNVERIFIED", "PENDING"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.commit(preview.getBatchId(), 3L));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
    }

    @Test
    void successfulCommitCreatesDraftQuestionOnlyOnce() {
        ImportPreviewResponse preview = preview("[" + standaloneQuestion("same") + "," + standaloneQuestion("same") + "]");
        assertEquals("VALIDATED", persistedBatch.getStatus());
        assertEquals(1, persistedItems.size());
        assertEquals("PENDING", persistedItems.get(0).getStatus());

        // A fresh instance exercises durable payload reloading, not process-local preview state.
        service = new ExamImportService(sourceMapper, questionMapper, batchMapper);
        service.commit(preview.getBatchId(), 3L);

        ArgumentCaptor<QuestionEntity> question = ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionMapper).insert(question.capture());
        assertEquals("same", question.getValue().getStem());
        assertEquals("DRAFT", question.getValue().getReviewStatus());
        assertEquals(9L, question.getValue().getSourceId());
        assertNull(question.getValue().getGroupId());
        assertEquals("IMPORTED", persistedItems.get(0).getStatus());
        assertTerminal("IMPORTED", 2, 1, 0, 1, 3L);
        verify(batchMapper).markImported(eq(101L), eq(3L), eq(1), eq(0), eq(1), eq("[\"row 2: duplicate content fingerprint\"]"));

        BusinessException replay = assertThrows(BusinessException.class, () -> service.commit(101L, 3L));
        assertEquals(ErrorCode.EXAM_IMPORT_INVALID, replay.getErrorCode());
        verify(questionMapper, times(1)).insert(any());
        verify(batchMapper, times(1)).markImported(anyLong(), anyLong(), anyInt(), anyInt(), anyInt(), anyString());
    }

    @Test
    void oneJsonGroupPersistsDistinctDeterministicItemRowNumbers() {
        ImportPreviewResponse preview = preview("[" + group() + "," + standaloneQuestion("standalone") + "]");

        assertTrue(preview.getErrors().isEmpty());
        assertEquals(3, preview.getValidRows());
        assertEquals(Arrays.asList(1, 2, 3), persistedItems.stream().map(ImportBatchItemEntity::getRowNumber).collect(Collectors.toList()));
        assertEquals(Arrays.asList(1, 2, 3), preview.getItems().stream().map(ImportPreviewResponse.Item::getRowNumber).collect(Collectors.toList()));
        assertEquals(3, persistedItems.stream().map(ImportBatchItemEntity::getRowNumber).distinct().count(),
                "exam_import_item UNIQUE(batch_id,row_number) must accept every member");
    }

    @Test
    void groupedCommitReloadsPayloadAndCreatesOneDraftGroupAndItsMembers() {
        preview("[" + group() + "]");
        service = new ExamImportService(sourceMapper, questionMapper, batchMapper);
        service.commit(101L, 7L);

        ArgumentCaptor<QuestionGroupEntity> groups = ArgumentCaptor.forClass(QuestionGroupEntity.class);
        verify(questionMapper).insertGroup(groups.capture());
        assertEquals("DRAFT", groups.getValue().getReviewStatus());
        assertEquals("passage", groups.getValue().getContent());
        ArgumentCaptor<QuestionEntity> questions = ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionMapper, times(2)).insert(questions.capture());
        assertEquals(Arrays.asList(1, 2), questions.getAllValues().stream().map(QuestionEntity::getGroupSortOrder).collect(Collectors.toList()));
        for (QuestionEntity question : questions.getAllValues()) {
            assertEquals("DRAFT", question.getReviewStatus());
            assertEquals(501L, question.getGroupId());
            assertEquals(9L, question.getSourceId());
        }
        assertTrue(persistedItems.stream().allMatch(item -> "IMPORTED".equals(item.getStatus())));
        assertTerminal("IMPORTED", 2, 2, 0, 0, 7L);
        assertEquals(ErrorCode.EXAM_IMPORT_INVALID, assertThrows(BusinessException.class, () -> service.commit(101L, 7L)).getErrorCode());
        verify(questionMapper, times(1)).insertGroup(any());
        verify(questionMapper, times(2)).insert(any());
    }

    @Test
    void reservesGroupAndQuestionFingerprintsInGlobalLexicographicOrderBeforeWrites() {
        preview("[" + group() + "," + standaloneQuestion("standalone") + "]");
        List<String> expected = new ArrayList<>();
        expected.add(persistedItems.get(0).getGroupFingerprint());
        persistedItems.forEach(item -> expected.add(item.getQuestionFingerprint()));
        Collections.sort(expected);
        assertEquals(4, new HashSet<>(expected).size());

        service.commit(101L, 3L);

        org.mockito.InOrder order = inOrder(batchMapper, questionMapper);
        for (String fingerprint : expected) {
            String kind = fingerprint.equals(persistedItems.get(0).getGroupFingerprint()) ? "GROUP" : "QUESTION";
            order.verify(batchMapper).reserveFingerprint(fingerprint, kind, 101L);
        }
        order.verify(questionMapper).insertGroup(any());
        verify(batchMapper, times(4)).reserveFingerprint(anyString(), anyString(), eq(101L));
    }

    @Test
    void groupConflictReleasesEveryNewlyAcquiredQuestionReservation() {
        preview("[" + group() + "]");
        String group = persistedItems.get(0).getGroupFingerprint();
        reservations.put(group, 900L);

        service.commit(101L, 3L);

        for (ImportBatchItemEntity item : persistedItems) verify(batchMapper).releaseFingerprint(item.getQuestionFingerprint(), 101L);
        verify(batchMapper, times(2)).releaseFingerprint(anyString(), eq(101L));
        assertEquals(Collections.singletonMap(group, 900L), reservations);
        assertNoQuestionOrGroupInserted();
        assertTerminal("IMPORTED", 2, 0, 0, 2, 3L);
    }

    @Test
    void groupConflictReleasesOnlyOwnedMembersAndPreservesPreExistingReservations() {
        preview("[" + group() + "]");
        String group = persistedItems.get(0).getGroupFingerprint();
        String existing = persistedItems.get(0).getQuestionFingerprint();
        String newlyAcquired = persistedItems.get(1).getQuestionFingerprint();
        reservations.put(group, 900L);
        reservations.put(existing, 901L);
        Map<String, Long> before = new HashMap<>(reservations);

        service.commit(101L, 3L);

        verify(batchMapper).releaseFingerprint(newlyAcquired, 101L);
        verify(batchMapper, times(1)).releaseFingerprint(anyString(), anyLong());
        assertEquals(before, reservations);
        assertNoQuestionOrGroupInserted();
        assertTerminal("IMPORTED", 2, 0, 0, 2, 3L);
    }

    @Test
    void allExistingStandaloneQuestionsDoNotInsertOrLeaveReservations() {
        preview("[" + standaloneQuestion("one") + "," + standaloneQuestion("two") + "]");
        persistedItems.forEach(item -> reservations.put(item.getQuestionFingerprint(), 900L));
        Map<String, Long> before = new HashMap<>(reservations);

        service.commit(101L, 3L);

        assertNoQuestionOrGroupInserted();
        assertEquals(before, reservations);
        verify(batchMapper, never()).releaseFingerprint(anyString(), anyLong());
        assertTerminal("IMPORTED", 2, 0, 0, 2, 3L);
    }

    @Test
    void allExistingGroupMembersReleaseTheUnusedNewGroupReservation() {
        preview("[" + group() + "]");
        persistedItems.forEach(item -> reservations.put(item.getQuestionFingerprint(), 900L));
        Map<String, Long> before = new HashMap<>(reservations);

        service.commit(101L, 3L);

        assertNoQuestionOrGroupInserted();
        assertEquals(before, reservations);
        verify(batchMapper).releaseFingerprint(persistedItems.get(0).getGroupFingerprint(), 101L);
        verify(batchMapper, times(1)).releaseFingerprint(anyString(), anyLong());
        assertTerminal("IMPORTED", 2, 0, 0, 2, 3L);
    }

    @Test
    void existingGroupAndAllItsExistingMembersKeepEveryPriorReservation() {
        preview("[" + group() + "]");
        reservations.put(persistedItems.get(0).getGroupFingerprint(), 900L);
        persistedItems.forEach(item -> reservations.put(item.getQuestionFingerprint(), 901L));
        Map<String, Long> before = new HashMap<>(reservations);

        service.commit(101L, 3L);

        assertNoQuestionOrGroupInserted();
        assertEquals(before, reservations);
        verify(batchMapper, never()).releaseFingerprint(anyString(), anyLong());
        assertTerminal("IMPORTED", 2, 0, 0, 2, 3L);
    }

    @Test
    void previewExistingStandaloneAndGroupDuplicatesCreateNoItemsOrReservations() {
        when(questionMapper.countQuestionsByContentFingerprint(anyString())).thenReturn(1L);
        when(questionMapper.countGroupsByContentFingerprint(anyString())).thenReturn(1L);
        ImportPreviewResponse preview = preview("[" + standaloneQuestion("existing") + "," + group() + "]");
        assertEquals(3, preview.getDuplicateRows());
        assertEquals(0, preview.getValidRows());
        assertTrue(persistedItems.isEmpty());

        service.commit(101L, 3L);

        assertNoQuestionOrGroupInserted();
        assertTrue(reservations.isEmpty());
        verify(batchMapper, never()).reserveFingerprint(anyString(), anyString(), anyLong());
        assertTerminal("IMPORTED", 3, 0, 0, 3, 3L);
    }

    @Test
    void failedUnusedGroupReleaseRaisesDataConflictAndRequestsTransactionRollback() {
        preview("[" + group() + "]");
        persistedItems.forEach(item -> reservations.put(item.getQuestionFingerprint(), 900L));
        String group = persistedItems.get(0).getGroupFingerprint();
        when(batchMapper.releaseFingerprint(group, 101L)).thenReturn(0);

        assertCleanupRollsBack();

        verify(batchMapper).releaseFingerprint(group, 101L);
    }

    @Test
    void failedOwnedMemberReleaseOnGroupConflictRequestsTransactionRollback() {
        preview("[" + group() + "]");
        reservations.put(persistedItems.get(0).getGroupFingerprint(), 900L);
        when(batchMapper.releaseFingerprint(persistedItems.get(0).getQuestionFingerprint(), 101L)).thenReturn(0);

        assertCleanupRollsBack();
    }

    @Test
    void rejectedFlattenedInvalidGroupAndPreviewDuplicateKeepTerminalCountsBalanced() {
        String invalidGroup = group().replace("\"title\":\"title\",", "");
        ImportPreviewResponse preview = preview("[" + standaloneQuestion("same") + "," + standaloneQuestion("same") + "," + invalidGroup + "]");
        assertEquals(4, preview.getTotalRows());
        assertEquals(1, preview.getDuplicateRows());
        assertFalse(preview.getErrors().isEmpty());
        assertTerminal("REJECTED", 4, 0, 3, 1, null);
        assertEquals(ErrorCode.EXAM_IMPORT_INVALID, assertThrows(BusinessException.class, () -> service.commit(101L, 3L)).getErrorCode());
        assertNoQuestionOrGroupInserted();
    }

    @Test
    void springCanConstructTheImportService() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(QuestionSourceMapper.class, () -> sourceMapper);
            context.registerBean(QuestionMapper.class, () -> questionMapper);
            context.registerBean(ImportBatchMapper.class, () -> batchMapper);
            context.register(ExamImportService.class);
            context.refresh();

            assertTrue(context.containsBean("examImportService"));
        }
    }

    private static QuestionSourceEntity source(String copyrightStatus, String reviewStatus) {
        QuestionSourceEntity source = new QuestionSourceEntity();
        source.setCopyrightStatus(copyrightStatus);
        source.setReviewStatus(reviewStatus);
        return source;
    }

    private static ImportBatchEntity batch(String status, long sourceId) {
        ImportBatchEntity batch = new ImportBatchEntity();
        batch.setId(101L);
        batch.setStatus(status);
        batch.setSourceId(sourceId);
        batch.setFailedRows(0);
        batch.setDuplicateWarnings("[]");
        return batch;
    }

    private int setItemStatus(long id, String status) {
        ImportBatchItemEntity item = persistedItems.stream().filter(row -> row.getId() == id).findFirst().orElseThrow();
        if (!"PENDING".equals(item.getStatus())) return 0;
        item.setStatus(status);
        return 1;
    }

    private ImportPreviewResponse preview(String json) {
        return service.preview(9L, "questions.json", json.getBytes(StandardCharsets.UTF_8));
    }

    private static String group() {
        return "{\"groupType\":\"READING\",\"title\":\"title\",\"content\":\"passage\",\"questions\":["
                + standaloneQuestion("first member").replace("VOCABULARY", "READING") + ","
                + standaloneQuestion("second member").replace("VOCABULARY", "READING") + "]}";
    }

    private void assertTerminal(String status, int total, int success, int failed, int duplicates, Long admin) {
        assertEquals(status, persistedBatch.getStatus());
        assertEquals(total, persistedBatch.getTotalRows());
        assertEquals(success, persistedBatch.getSuccessRows());
        assertEquals(failed, persistedBatch.getFailedRows());
        assertEquals(duplicates, persistedBatch.getDuplicateRows());
        assertEquals(admin, persistedBatch.getImportedBy());
        assertEquals(persistedBatch.getTotalRows(), persistedBatch.getSuccessRows() + persistedBatch.getFailedRows() + persistedBatch.getDuplicateRows());
    }

    private void assertNoQuestionOrGroupInserted() {
        verify(questionMapper, never()).insert(any());
        verify(questionMapper, never()).insertGroup(any());
    }

    private void assertCleanupRollsBack() {
        PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
        SimpleTransactionStatus transaction = new SimpleTransactionStatus();
        when(transactions.getTransaction(any())).thenReturn(transaction);
        ProxyFactory proxy = new ProxyFactory(service);
        TransactionInterceptor interceptor = new TransactionInterceptor();
        interceptor.setTransactionManager(transactions);
        interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        proxy.addAdvice(interceptor);
        ExamImportService transactionalService = (ExamImportService) proxy.getProxy();

        BusinessException failure = assertThrows(BusinessException.class, () -> transactionalService.commit(101L, 3L));

        assertEquals(ErrorCode.DATA_CONFLICT, failure.getErrorCode());
        verify(transactions).rollback(transaction);
        verify(transactions, never()).commit(any());
        verify(batchMapper, never()).markImported(anyLong(), anyLong(), anyInt(), anyInt(), anyInt(), anyString());
        assertEquals("VALIDATED", persistedBatch.getStatus());
        assertNoQuestionOrGroupInserted();
    }

    private static String jsonQuestion() {
        return "[" + standaloneQuestion("A word with \u00e9") + "]";
    }

    private static String standaloneQuestion(String stem) {
        return "{\"questionType\":\"VOCABULARY\",\"stem\":\"" + stem + "\","
                + "\"options\":{\"A\":\"one\",\"B\":\"two\",\"C\":\"three\",\"D\":\"four\"},"
                + "\"correctAnswer\":\"A\",\"explanation\":\"why this answer is correct\","
                + "\"difficulty\":2,\"knowledgePoints\":[\"vocabulary\"]}";
    }

    private static String csvQuestion() {
        return "questionType,stem,options,correctAnswer,explanation,difficulty,knowledgePoints\n"
                + "VOCABULARY,\"Which line?\nline two\",\"{\"\"A\"\":\"\"one\"\",\"\"B\"\":\"\"two\"\",\"\"C\"\":\"\"three\"\",\"\"D\"\":\"\"four\"\"}\",A,\"explains answer\",2,\"[\"\"vocabulary\"\"]\"\n";
    }
}

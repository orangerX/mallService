package com.mall.exam.importer;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import com.mall.exam.importer.model.ImportBatchEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamImportServiceTest {

    @Mock QuestionSourceMapper sourceMapper;
    @Mock QuestionMapper questionMapper;
    @Mock ImportBatchMapper batchMapper;
    private ExamImportService service;

    @BeforeEach
    void setUp() {
        service = new ExamImportService(sourceMapper, questionMapper, batchMapper);
        org.mockito.Mockito.lenient().when(batchMapper.insertBatch(any(ImportBatchEntity.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, ImportBatchEntity.class).setId(101L);
            return 1;
        });
        org.mockito.Mockito.lenient().when(batchMapper.insertItem(any())).thenReturn(1);
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
        ImportPreviewResponse preview = service.preview(9L, "valid.json", jsonQuestion().getBytes(StandardCharsets.UTF_8));
        when(batchMapper.findBatchByIdForUpdate(101L)).thenReturn(batch("VALIDATED", 9L));
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source("ORIGINAL", "APPROVED"));
        when(batchMapper.findItemsByBatchIdForUpdate(101L)).thenReturn(java.util.Collections.singletonList(batchItem(jsonQuestion())));
        when(batchMapper.reserveFingerprint(any(), any(), anyLong())).thenReturn(1);
        when(batchMapper.markItemImported(anyLong())).thenReturn(1);
        when(batchMapper.markImported(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt(), any())).thenReturn(1);
        when(batchMapper.findBatchByIdForUpdate(101L)).thenReturn(batch("VALIDATED", 9L), batch("IMPORTED", 9L));
        when(questionMapper.insert(any(QuestionEntity.class))).thenReturn(1);

        service.commit(preview.getBatchId(), 3L);
        BusinessException replay = assertThrows(BusinessException.class, () -> service.commit(preview.getBatchId(), 3L));

        ArgumentCaptor<QuestionEntity> question = ArgumentCaptor.forClass(QuestionEntity.class);
        verify(questionMapper, times(1)).insert(question.capture());
        assertEquals("DRAFT", question.getValue().getReviewStatus());
        assertEquals(1, question.getValue().getEnabled());
        assertEquals(ErrorCode.EXAM_IMPORT_INVALID, replay.getErrorCode());
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
        return batch;
    }

    private static com.mall.exam.importer.model.ImportBatchItemEntity batchItem(String payload) {
        com.mall.exam.importer.model.ImportBatchItemEntity item = new com.mall.exam.importer.model.ImportBatchItemEntity();
        item.setPayload(payload.substring(1, payload.length() - 1));
        item.setId(201L);
        item.setQuestionFingerprint(QuestionFingerprint.sha256("", "A word with é"));
        item.setStatus("PENDING");
        return item;
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

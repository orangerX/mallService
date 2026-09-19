package com.mall.exam.importer;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import com.mall.exam.importer.dto.ImportPreviewResponse;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamImportServiceTest {

    @Mock QuestionSourceMapper sourceMapper;
    @Mock QuestionMapper questionMapper;
    private ExamImportService service;

    @BeforeEach
    void setUp() {
        service = new ExamImportService(sourceMapper, questionMapper);
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
        assertEquals(1, preview.getErrors().size());
        assertTrue(preview.getErrors().get(0).contains("row 2"));
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
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source("UNVERIFIED", "PENDING"));

        BusinessException error = assertThrows(BusinessException.class, () -> service.commit(preview.getBatchId(), 3L));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
    }

    @Test
    void successfulCommitCreatesDraftQuestionOnlyOnce() {
        ImportPreviewResponse preview = service.preview(9L, "valid.json", jsonQuestion().getBytes(StandardCharsets.UTF_8));
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source("ORIGINAL", "APPROVED"));
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

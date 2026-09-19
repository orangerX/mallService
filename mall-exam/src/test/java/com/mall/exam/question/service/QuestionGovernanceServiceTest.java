package com.mall.exam.question.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionGovernanceServiceTest {

    @Mock QuestionSourceMapper sourceMapper;
    @Mock QuestionMapper questionMapper;
    private QuestionGovernanceService service;

    @BeforeEach
    void setUp() {
        service = new QuestionGovernanceService(sourceMapper, questionMapper);
    }

    @Test
    void cannotApproveQuestionWhoseSourceIsUnverified() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "UNVERIFIED", "PENDING"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewQuestion(21L, 3L, approveCommand()));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
        verify(questionMapper, never()).updateReview(anyLong(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void approvingDialogueQuestionReviewsItsWholeGroupAtomically() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "ORIGINAL", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));

        service.reviewQuestion(21L, 3L, approveCommand());

        verify(questionMapper).updateGroupReview(7L, "APPROVED");
        verify(questionMapper).updateReviewByGroupId(eq(7L), eq("APPROVED"), eq(3L), any(),
                eq("内容符合发布条件"));
        verify(questionMapper, never()).updateReview(anyLong(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void approvingSingleQuestionUpdatesOnlyThatQuestion() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "AUTHORIZED", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));

        service.reviewQuestion(21L, 3L, approveCommand());

        verify(questionMapper).updateReview(eq(21L), eq("APPROVED"), eq(3L), any(),
                eq("内容符合发布条件"));
        verify(questionMapper, never()).updateGroupReview(anyLong(), anyString());
        verify(questionMapper, never()).updateReviewByGroupId(anyLong(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void changingGroupStatusUpdatesGroupAndEveryMember() {
        service.changeGroupStatus(7L, 0);

        verify(questionMapper).updateGroupStatus(7L, 0);
        verify(questionMapper).updateStatusByGroupId(7L, 0);
    }

    @Test
    void changingGroupedQuestionStatusUpdatesItsWholeGroup() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));

        service.changeQuestionStatus(21L, 0);

        verify(questionMapper).updateGroupStatus(7L, 0);
        verify(questionMapper).updateStatusByGroupId(7L, 0);
        verify(questionMapper, never()).updateStatus(anyLong(), any());
    }

    @Test
    void changingSingleQuestionStatusUpdatesOnlyThatQuestion() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));

        service.changeQuestionStatus(21L, 0);

        verify(questionMapper).updateStatus(21L, 0);
        verify(questionMapper, never()).updateStatusByGroupId(anyLong(), any());
    }

    @Test
    void disablingReferencedQuestionIsAllowedButDeletingItIsRejected() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.countPaperItemSnapshots(21L)).thenReturn(1L);

        service.changeQuestionStatus(21L, 0);
        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteQuestion(21L));

        assertEquals(ErrorCode.DATA_CONFLICT, error.getErrorCode());
        verify(questionMapper).updateStatus(21L, 0);
        verify(questionMapper, never()).deleteUnreferencedById(21L);
    }

    private static QuestionReviewCommand approveCommand() {
        return new QuestionReviewCommand(true, "内容符合发布条件");
    }

    private static QuestionEntity question(Long id, Long groupId, Long sourceId) {
        QuestionEntity question = new QuestionEntity();
        question.setId(id);
        question.setGroupId(groupId);
        question.setSourceId(sourceId);
        return question;
    }

    private static QuestionSourceEntity source(Long id, String copyrightStatus, String reviewStatus) {
        QuestionSourceEntity source = new QuestionSourceEntity();
        source.setId(id);
        source.setCopyrightStatus(copyrightStatus);
        source.setReviewStatus(reviewStatus);
        return source;
    }
}

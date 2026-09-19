package com.mall.exam.question.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.question.model.QuestionGroupEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        verify(questionMapper, never()).approveQuestionIfSourceReusable(anyLong());
    }

    @Test
    void approvingDialogueQuestionReviewsItsWholeGroupAtomically() {
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source(9L, "ORIGINAL", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));
        when(questionMapper.findGroupByIdForUpdate(7L)).thenReturn(group(7L, 9L));
        when(questionMapper.findByGroupIdForUpdate(7L)).thenReturn(Collections.singletonList(question(21L, 7L, 9L)));
        when(questionMapper.approveGroupIfReusable(7L)).thenReturn(1);
        when(questionMapper.approveQuestionsByGroupIfReusable(7L)).thenReturn(1);

        service.reviewQuestion(21L, 3L, approveCommand());

        verify(questionMapper).approveGroupIfReusable(7L);
        verify(questionMapper).approveQuestionsByGroupIfReusable(7L);
    }

    @Test
    void approvingSingleQuestionUpdatesOnlyThatQuestion() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "AUTHORIZED", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.approveQuestionIfSourceReusable(21L)).thenReturn(1);

        service.reviewQuestion(21L, 3L, approveCommand());

        verify(questionMapper).approveQuestionIfSourceReusable(21L);
        verify(questionMapper, never()).approveGroupIfReusable(anyLong());
        verify(questionMapper, never()).approveQuestionsByGroupIfReusable(anyLong());
    }

    @Test
    void rollsBackGroupApprovalWhenGuardedMutationDoesNotCoverEveryMember() {
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source(9L, "ORIGINAL", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));
        when(questionMapper.findGroupByIdForUpdate(7L)).thenReturn(group(7L, 9L));
        when(questionMapper.findByGroupIdForUpdate(7L)).thenReturn(Arrays.asList(
                question(21L, 7L, 9L), question(22L, 7L, 9L)));
        when(questionMapper.approveGroupIfReusable(7L)).thenReturn(1);
        when(questionMapper.approveQuestionsByGroupIfReusable(7L)).thenReturn(1);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewQuestion(21L, 3L, approveCommand()));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
    }

    @Test
    void cannotApproveGroupWhenASiblingUsesADifferentSource() {
        when(sourceMapper.findByIdForUpdate(9L)).thenReturn(source(9L, "ORIGINAL", "APPROVED"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));
        when(questionMapper.findGroupByIdForUpdate(7L)).thenReturn(group(7L, 9L));
        when(questionMapper.findByGroupIdForUpdate(7L)).thenReturn(Arrays.asList(
                question(21L, 7L, 9L), question(22L, 7L, 10L)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewQuestion(21L, 3L, approveCommand()));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
        verify(questionMapper, never()).approveGroupIfReusable(anyLong());
        verify(questionMapper, never()).approveQuestionsByGroupIfReusable(anyLong());
    }

    @Test
    void cannotApproveGroupWhenItsOwnSourceDiffersFromItsMembers() {
        when(sourceMapper.findByIdForUpdate(10L)).thenReturn(source(10L, "UNVERIFIED", "PENDING"));
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));
        when(questionMapper.findGroupByIdForUpdate(7L)).thenReturn(group(7L, 10L));
        when(questionMapper.findByGroupIdForUpdate(7L)).thenReturn(Collections.singletonList(question(21L, 7L, 9L)));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewQuestion(21L, 3L, approveCommand()));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
        verify(questionMapper, never()).approveGroupIfReusable(anyLong());
    }

    @Test
    void cannotApproveUnverifiedSourceByReusingItsStoredCopyrightStatus() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "UNVERIFIED", "PENDING"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewSource(9L, 3L, new SourceReviewCommand(true, "不能批准")));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
        verify(sourceMapper, never()).approveSourceWithReusableCopyright(anyLong(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void cannotApproveSourceWithExplicitProhibitedCopyrightStatus() {
        when(sourceMapper.findById(9L)).thenReturn(source(9L, "ORIGINAL", "PENDING"));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewSource(9L, 3L,
                        new SourceReviewCommand(true, "PROHIBITED", "禁止发布")));

        assertEquals(ErrorCode.EXAM_SOURCE_UNAPPROVED, error.getErrorCode());
        verify(sourceMapper, never()).approveSourceWithReusableCopyright(anyLong(), anyString(), anyLong(), any(), anyString());
    }

    @Test
    void changingGroupStatusUpdatesGroupAndEveryMember() {
        when(questionMapper.updateGroupStatus(7L, 0)).thenReturn(1);
        when(questionMapper.updateStatusByGroupId(7L, 0)).thenReturn(1);

        service.changeGroupStatus(7L, 0);

        verify(questionMapper).updateGroupStatus(7L, 0);
        verify(questionMapper).updateStatusByGroupId(7L, 0);
    }

    @Test
    void rejectsInvalidGroupStatusBeforeMutatingRows() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.changeGroupStatus(7L, -1));

        assertEquals(ErrorCode.VALIDATION_ERROR, error.getErrorCode());
        verify(questionMapper, never()).updateGroupStatus(anyLong(), any());
        verify(questionMapper, never()).updateStatusByGroupId(anyLong(), any());
    }

    @Test
    void changingGroupedQuestionStatusUpdatesItsWholeGroup() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, 7L, 9L));
        when(questionMapper.updateGroupStatus(7L, 0)).thenReturn(1);
        when(questionMapper.updateStatusByGroupId(7L, 0)).thenReturn(1);

        service.changeQuestionStatus(21L, 0);

        verify(questionMapper).updateGroupStatus(7L, 0);
        verify(questionMapper).updateStatusByGroupId(7L, 0);
        verify(questionMapper, never()).updateStatus(anyLong(), any());
    }

    @Test
    void changingSingleQuestionStatusUpdatesOnlyThatQuestion() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.updateStatus(21L, 0)).thenReturn(1);

        service.changeQuestionStatus(21L, 0);

        verify(questionMapper).updateStatus(21L, 0);
        verify(questionMapper, never()).updateStatusByGroupId(anyLong(), any());
    }

    @Test
    void rejectsInvalidSingleQuestionStatusBeforeLoadingOrMutatingRows() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.changeQuestionStatus(21L, 2));

        assertEquals(ErrorCode.VALIDATION_ERROR, error.getErrorCode());
        verify(questionMapper, never()).findById(anyLong());
        verify(questionMapper, never()).updateStatus(anyLong(), any());
    }

    @Test
    void rejectsStandaloneStatusChangeWhenStandaloneGuardAffectsNoRows() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.updateStatus(21L, 0)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.changeQuestionStatus(21L, 0));

        assertEquals(ErrorCode.DATA_CONFLICT, error.getErrorCode());
    }

    @Test
    void rejectsGroupStatusChangeWhenGroupOrMemberUpdateAffectsNoRows() {
        when(questionMapper.updateGroupStatus(7L, 0)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class, () -> service.changeGroupStatus(7L, 0));

        assertEquals(ErrorCode.DATA_CONFLICT, error.getErrorCode());
        verify(questionMapper, never()).updateStatusByGroupId(anyLong(), any());
    }

    @Test
    void rejectsStandaloneRejectionWhenStandaloneGuardAffectsNoRows() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.rejectQuestion(21L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.reviewQuestion(21L, 3L, new QuestionReviewCommand(false, "驳回")));

        assertEquals(ErrorCode.DATA_CONFLICT, error.getErrorCode());
    }

    @Test
    void mapperDoesNotExposeRawApprovalMutations() {
        assertFalse(Arrays.stream(QuestionMapper.class.getMethods())
                .anyMatch(method -> method.getName().equals("updateReview") || method.getName().equals("updateGroupReview")
                        || method.getName().equals("updateReviewByGroupId")));
        assertFalse(Arrays.stream(QuestionSourceMapper.class.getMethods())
                .anyMatch(method -> method.getName().equals("updateReview")));
    }

    @Test
    void singleQuestionMapperMutationsRequireStandaloneRows() throws Exception {
        String xml = questionMapperXml();

        assertMutationHasStandaloneGuard(xml, "approveQuestionIfSourceReusable", "question.group_id IS NULL");
        assertMutationHasStandaloneGuard(xml, "rejectQuestion", "group_id IS NULL");
        assertMutationHasStandaloneGuard(xml, "updateStatus", "group_id IS NULL");
        assertMutationHasStandaloneGuard(xml, "deleteUnreferencedById", "group_id IS NULL");
    }

    @Test
    void disablingReferencedQuestionIsAllowedButDeletingItIsRejected() {
        when(questionMapper.findById(21L)).thenReturn(question(21L, null, 9L));
        when(questionMapper.updateStatus(21L, 0)).thenReturn(1);
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

    private static QuestionGroupEntity group(Long id, Long sourceId) {
        QuestionGroupEntity group = new QuestionGroupEntity();
        group.setId(id);
        group.setSourceId(sourceId);
        return group;
    }

    private static QuestionSourceEntity source(Long id, String copyrightStatus, String reviewStatus) {
        QuestionSourceEntity source = new QuestionSourceEntity();
        source.setId(id);
        source.setCopyrightStatus(copyrightStatus);
        source.setReviewStatus(reviewStatus);
        return source;
    }

    private static String questionMapperXml() throws Exception {
        try (InputStream input = QuestionGovernanceServiceTest.class
                .getResourceAsStream("/mapper/exam/QuestionMapper.xml")) {
            if (input == null) throw new IllegalStateException("QuestionMapper.xml not found on classpath");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void assertMutationHasStandaloneGuard(String xml, String statementId, String guard) {
        String openingTag = "<(?:update|delete) id=\"" + statementId + "\">";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(openingTag).matcher(xml);
        assertTrue(matcher.find(), statementId + " statement must exist");
        int start = matcher.start();
        int end = xml.indexOf("</", start);
        assertTrue(end > start && xml.substring(start, end).contains(guard),
                statementId + " must be limited to standalone questions");
    }
}

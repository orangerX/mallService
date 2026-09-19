package com.mall.exam.question.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.question.mapper.QuestionMapper;
import com.mall.exam.question.model.QuestionEntity;
import com.mall.exam.source.mapper.QuestionSourceMapper;
import com.mall.exam.source.model.QuestionSourceEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class QuestionGovernanceService {

    private static final Set<String> REUSABLE_COPYRIGHT_STATUSES = new HashSet<>(
            Arrays.asList("ORIGINAL", "AUTHORIZED", "PUBLIC_OFFICIAL"));

    private final QuestionSourceMapper sourceMapper;
    private final QuestionMapper questionMapper;

    public QuestionGovernanceService(QuestionSourceMapper sourceMapper, QuestionMapper questionMapper) {
        this.sourceMapper = sourceMapper;
        this.questionMapper = questionMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewSource(long sourceId, long adminId, SourceReviewCommand command) {
        QuestionSourceEntity source = requiredSource(sourceId);
        String copyrightStatus = hasText(command.getCopyrightStatus())
                ? command.getCopyrightStatus() : source.getCopyrightStatus();
        String reviewStatus = command.isApproved() ? "APPROVED" : "REJECTED";
        sourceMapper.updateReview(sourceId, copyrightStatus, reviewStatus, adminId,
                LocalDateTime.now(), command.getReviewNote());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewQuestion(long questionId, long adminId, QuestionReviewCommand command) {
        QuestionEntity question = requiredQuestion(questionId);
        String reviewStatus = command.isApproved() ? "APPROVED" : "REJECTED";
        if (command.isApproved() && !reusable(requiredSource(question.getSourceId()))) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
        }

        LocalDateTime reviewedAt = LocalDateTime.now();
        if (question.getGroupId() != null) {
            questionMapper.updateGroupReview(question.getGroupId(), reviewStatus);
            questionMapper.updateReviewByGroupId(question.getGroupId(), reviewStatus, adminId,
                    reviewedAt, command.getReviewNote());
            return;
        }
        questionMapper.updateReview(questionId, reviewStatus, adminId, reviewedAt, command.getReviewNote());
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeGroupStatus(long groupId, int status) {
        questionMapper.updateGroupStatus(groupId, status);
        questionMapper.updateStatusByGroupId(groupId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeQuestionStatus(long questionId, int status) {
        QuestionEntity question = requiredQuestion(questionId);
        if (question.getGroupId() != null) {
            changeGroupStatus(question.getGroupId(), status);
            return;
        }
        questionMapper.updateStatus(questionId, status);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteQuestion(long questionId) {
        requiredQuestion(questionId);
        if (questionMapper.countPaperItemSnapshots(questionId) > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT,
                    "题目已被试卷快照引用，只能停用");
        }
        if (questionMapper.deleteUnreferencedById(questionId) == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT,
                    "题目已被试卷快照引用，只能停用");
        }
    }

    private QuestionEntity requiredQuestion(long questionId) {
        QuestionEntity question = questionMapper.findById(questionId);
        if (question == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
        }
        return question;
    }

    private QuestionSourceEntity requiredSource(Long sourceId) {
        QuestionSourceEntity source = sourceId == null ? null : sourceMapper.findById(sourceId);
        if (source == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
        }
        return source;
    }

    private static boolean reusable(QuestionSourceEntity source) {
        return "APPROVED".equals(source.getReviewStatus())
                && REUSABLE_COPYRIGHT_STATUSES.contains(source.getCopyrightStatus());
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

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
        LocalDateTime reviewedAt = LocalDateTime.now();
        if (command.isApproved()) {
            if (!reusableCopyright(copyrightStatus)
                    || sourceMapper.approveSourceWithReusableCopyright(sourceId, copyrightStatus, adminId,
                    reviewedAt, command.getReviewNote()) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
            }
            return;
        }
        sourceMapper.rejectSource(sourceId, copyrightStatus, adminId, reviewedAt, command.getReviewNote());
    }

    @Transactional(rollbackFor = Exception.class)
    public void reviewQuestion(long questionId, long adminId, QuestionReviewCommand command) {
        QuestionEntity question = requiredQuestion(questionId);
        if (question.getGroupId() != null) {
            reviewGroup(questionId, question.getGroupId(), command.isApproved());
            return;
        }
        if (command.isApproved()) {
            if (!reusable(requiredSource(question.getSourceId()))
                    || questionMapper.approveQuestionIfSourceReusable(questionId) != 1) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
            }
            return;
        }
        requireAffected(questionMapper.rejectQuestion(questionId));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeGroupStatus(long groupId, int status) {
        requireEnabledStatus(status);
        requireAffected(questionMapper.updateGroupStatus(groupId, status));
        requireAnyAffected(questionMapper.updateStatusByGroupId(groupId, status));
    }

    @Transactional(rollbackFor = Exception.class)
    public void changeQuestionStatus(long questionId, int status) {
        requireEnabledStatus(status);
        QuestionEntity question = requiredQuestion(questionId);
        if (question.getGroupId() != null) {
            changeGroupStatus(question.getGroupId(), status);
            return;
        }
        requireAffected(questionMapper.updateStatus(questionId, status));
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

    private QuestionSourceEntity requiredSourceForUpdate(Long sourceId) {
        QuestionSourceEntity source = sourceId == null ? null : sourceMapper.findByIdForUpdate(sourceId);
        if (source == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
        }
        return source;
    }

    private void reviewGroup(long questionId, Long groupId, boolean approved) {
        com.mall.exam.question.model.QuestionGroupEntity group = questionMapper.findGroupByIdForUpdate(groupId);
        if (group == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
        }
        java.util.List<QuestionEntity> members = questionMapper.findByGroupIdForUpdate(groupId);
        if (members.isEmpty() || members.stream().noneMatch(member -> member.getId().equals(questionId))) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
        }
        if (!approved) {
            requireAffected(questionMapper.rejectGroup(groupId));
            if (questionMapper.rejectQuestionsByGroupId(groupId) != members.size()) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
            }
            return;
        }

        QuestionSourceEntity groupSource = requiredSourceForUpdate(group.getSourceId());
        if (!reusable(groupSource)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
        }
        for (QuestionEntity member : members) {
            if (!group.getSourceId().equals(member.getSourceId())
                    || !reusable(requiredSourceForUpdate(member.getSourceId()))) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
            }
        }
        if (questionMapper.approveGroupIfReusable(groupId) != 1
                || questionMapper.approveQuestionsByGroupIfReusable(groupId) != members.size()) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_SOURCE_UNAPPROVED);
        }
    }

    private static boolean reusable(QuestionSourceEntity source) {
        return "APPROVED".equals(source.getReviewStatus())
                && reusableCopyright(source.getCopyrightStatus());
    }

    private static boolean reusableCopyright(String copyrightStatus) {
        return REUSABLE_COPYRIGHT_STATUSES.contains(copyrightStatus);
    }

    private static void requireEnabledStatus(int status) {
        if (status != 0 && status != 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR);
        }
    }

    private static void requireAffected(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
        }
    }

    private static void requireAnyAffected(int affectedRows) {
        if (affectedRows <= 0) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.DATA_CONFLICT);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

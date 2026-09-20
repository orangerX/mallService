package com.mall.exam.attempt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.TextNode;
import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.attempt.dto.*;
import com.mall.exam.attempt.mapper.ExamAttemptMapper;
import com.mall.exam.attempt.model.*;
import com.mall.exam.paper.dto.ExamPaperResponse;
import com.mall.exam.paper.mapper.ExamPaperMapper;
import com.mall.exam.paper.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExamAttemptService {
    private final ExamAttemptMapper mapper;
    private final ExamPaperMapper papers;
    private final ExamScoringService scoring;
    private final ObjectMapper json;
    private final Clock clock;
    private final TransactionTemplate transaction;

    public ExamAttemptService(ExamAttemptMapper mapper, ExamPaperMapper papers, ExamScoringService scoring,
                              ObjectMapper json, Clock clock, PlatformTransactionManager transactions) {
        this.mapper = mapper; this.papers = papers; this.scoring = scoring; this.json = json; this.clock = clock;
        transaction = new TransactionTemplate(transactions);
        // Expiry discovered by save must commit before the caller receives EXAM_EXPIRED,
        // even when this service is invoked from another transaction.
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public ExamPaperResponse current(long userId) {
        return transaction.execute(status -> {
            ExamPaperEntity candidate = papers.findInProgress(userId);
            return candidate == null ? null : currentLocked(ownedPaper(userId, candidate.getId()));
        });
    }

    /** Used to resume a candidate found by the generation service, with fresh status and ownership. */
    public ExamPaperResponse current(long userId, long paperId) {
        return transaction.execute(status -> currentLocked(ownedPaper(userId, paperId)));
    }

    private ExamPaperResponse currentLocked(ExamPaperEntity paper) {
        LocalDateTime now = LocalDateTime.now(clock);
        expire(paper, now);
        List<SavedAnswerResponse> answers = mapper.findAnswers(paper.getId()).stream()
                .map(answer -> savedResponse(paper, answer.getPaperItemId(), answer, now)).collect(Collectors.toList());
        return new ExamPaperResponse(paper, papers.findGroupSnapshots(paper.getId()), papers.findItemSnapshots(paper.getId()),
                now, answers, submitted(paper) ? new ExamResultResponse(paper, now) : null);
    }

    public SavedAnswerResponse save(long userId, SaveAnswerRequest request) {
        if (request == null || request.paperId == null || request.paperId <= 0 || request.paperItemId == null
                || request.paperItemId <= 0 || request.answerVersion == null || request.answerVersion < 0)
            throw invalidRequest();
        SaveOutcome outcome = transaction.execute(status -> {
            ExamPaperEntity paper = ownedPaper(userId, request.paperId);
            LocalDateTime now = LocalDateTime.now(clock);
            if (submitted(paper)) throw conflict(ErrorCode.EXAM_ALREADY_SUBMITTED);
            if (expired(paper, now)) {
                completeLocked(paper, true, now);
                return new SaveOutcome(null, ErrorCode.EXAM_EXPIRED);
            }
            ExamPaperItemSnapshotEntity item = mapper.findPaperItem(paper.getId(), request.paperItemId);
            if (item == null || !paper.getId().equals(item.getPaperId())
                    || !request.paperItemId.equals(item.getId()))
                throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
            if (!scoring.isObjective(item.getQuestionType()) && !scoring.isSubjective(item.getQuestionType()))
                throw invalidRequest();
            ExamAnswerEntity answer = mapper.findAnswerForUpdate(paper.getId(), item.getId());
            int version = answer == null ? 0 : answer.getAnswerVersion();
            if (request.answerVersion != version)
                throw new ExamAnswerConflictException(savedResponse(paper, item.getId(), answer, now));
            if (version == Integer.MAX_VALUE) throw conflict(ErrorCode.DATA_CONFLICT);
            validateContent(item.getQuestionType(), request.answerContent);
            boolean insert = answer == null;
            if (insert) {
                answer = new ExamAnswerEntity();
                answer.setPaperId(paper.getId()); answer.setPaperItemId(item.getId());
            }
            answer.setAnswerContent(request.answerContent == null || request.answerContent.isNull()
                    ? null : request.answerContent.toString());
            answer.setAnswerVersion(version + 1); answer.setSavedAt(now);
            if (insert) requireOne(mapper.insertAnswer(answer));
            else if (mapper.updateAnswer(answer, version) != 1) {
                ExamAnswerEntity latest = mapper.findAnswerForUpdate(paper.getId(), item.getId());
                throw new ExamAnswerConflictException(savedResponse(paper, item.getId(), latest, now));
            }
            return new SaveOutcome(savedResponse(paper, item.getId(), answer, now), null);
        });
        // Deliberately outside the transaction callback: throwing inside would undo auto-submission.
        if (outcome.error != null) throw conflict(outcome.error);
        return outcome.answer;
    }

    public ExamResultResponse submit(long userId, long paperId, boolean automatic) {
        return transaction.execute(status -> {
            ExamPaperEntity paper = ownedPaper(userId, paperId);
            LocalDateTime now = LocalDateTime.now(clock);
            if (!submitted(paper)) {
                boolean expired = expired(paper, now);
                // A client's fast clock cannot force an early automatic submission.
                if (automatic && !expired) throw invalidRequest();
                completeLocked(paper, expired, now);
            }
            return new ExamResultResponse(paper, now);
        });
    }

    public ExamReviewResponse review(long userId, long paperId) {
        return transaction.execute(status -> {
            ExamPaperEntity paper = ownedPaper(userId, paperId);
            LocalDateTime now = LocalDateTime.now(clock);
            expire(paper, now);
            requireSubmitted(paper);
            Map<Long, ExamAnswerEntity> answers = mapper.findAnswers(paperId).stream()
                    .collect(Collectors.toMap(ExamAnswerEntity::getPaperItemId, answer -> answer));
            Map<Long, ExamWrongRecordEntity> wrong = mapper.findWrongRecords(userId, paperId).stream()
                    .collect(Collectors.toMap(ExamWrongRecordEntity::getPaperItemId, record -> record));
            List<ExamReviewResponse.WrongItem> wrongItems = new ArrayList<>();
            List<ExamReviewResponse.SubjectiveItem> subjectiveItems = new ArrayList<>();
            Set<Long> groupIds = new HashSet<>();
            for (ExamPaperItemSnapshotEntity item : papers.findItemSnapshots(paperId)) {
                if (scoring.isObjective(item.getQuestionType()) && wrong.containsKey(item.getId())) {
                    String answer = wrong.get(item.getId()).getWrongAnswer();
                    wrongItems.add(new ExamReviewResponse.WrongItem(item, readContent(answer), readContent(item.getCorrectAnswer()),
                            scoring.normalize(answer).isEmpty()));
                    if (item.getPaperGroupSnapshotId() != null) groupIds.add(item.getPaperGroupSnapshotId());
                } else if (scoring.isSubjective(item.getQuestionType())) {
                    ExamAnswerEntity answer = answers.get(item.getId());
                    subjectiveItems.add(new ExamReviewResponse.SubjectiveItem(item,
                            readContent(answer == null ? null : answer.getAnswerContent())));
                }
            }
            List<ExamPaperResponse.Group> groups = papers.findGroupSnapshots(paperId).stream()
                    .filter(group -> groupIds.contains(group.getId())).map(ExamPaperResponse.Group::new).collect(Collectors.toList());
            return new ExamReviewResponse(new ExamResultResponse(paper, now), groups, wrongItems, subjectiveItems);
        });
    }

    public ExamResultResponse selfScore(long userId, SelfScoreRequest request) {
        if (request == null || request.paperId == null || request.paperId <= 0) throw invalidRequest();
        return transaction.execute(status -> {
            ExamPaperEntity paper = ownedPaper(userId, request.paperId);
            LocalDateTime now = LocalDateTime.now(clock);
            expire(paper, now);
            requireSubmitted(paper);
            int translation = selfScoreValue(request.translationScore);
            int writing = selfScoreValue(request.writingScore);
            BigDecimal total = paper.getObjectiveScore().add(BigDecimal.valueOf(translation + writing));
            if (!Integer.valueOf(translation).equals(paper.getTranslationSelfScore())
                    || !Integer.valueOf(writing).equals(paper.getWritingSelfScore())
                    || paper.getSelfScoreTotal() == null || total.compareTo(paper.getSelfScoreTotal()) != 0) {
                paper.setTranslationSelfScore(translation); paper.setWritingSelfScore(writing);
                paper.setSelfScoreTotal(total);
                paper.setUpdatedAt(now);
                requireOne(mapper.updateSelfScore(paper));
                paper.setVersion(paper.getVersion() + 1);
            }
            return new ExamResultResponse(paper, now);
        });
    }

    private int selfScoreValue(JsonNode value) {
        if (value == null || !value.isIntegralNumber() || !value.canConvertToInt() || value.intValue() < 0 || value.intValue() > 15)
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.EXAM_INVALID_SELF_SCORE);
        return value.intValue();
    }

    private void validateContent(String type, JsonNode content) {
        if (scoring.isObjective(type)) {
            if (!scoring.isValidObjectiveAnswer(content)) throw invalidRequest();
        } else if (content != null && !content.isNull() && (!content.isTextual() || content.asText().length() > 20000)) {
            throw invalidRequest();
        }
    }

    private ExamPaperEntity ownedPaper(long userId, long paperId) {
        ExamPaperEntity paper = mapper.findPaperForUpdate(userId, paperId);
        if (paper == null || !Long.valueOf(userId).equals(paper.getUserId()) || !Long.valueOf(paperId).equals(paper.getId()))
            throw new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.EXAM_NOT_FOUND);
        if (!"IN_PROGRESS".equals(paper.getStatus()) && !submitted(paper))
            throw new IllegalStateException("Unknown exam paper status");
        return paper;
    }

    private boolean submitted(ExamPaperEntity paper) {
        return "SUBMITTED".equals(paper.getStatus()) || "AUTO_SUBMITTED".equals(paper.getStatus());
    }

    private boolean expired(ExamPaperEntity paper, LocalDateTime now) {
        return !now.isBefore(paper.getDueAt());
    }

    private void expire(ExamPaperEntity paper, LocalDateTime now) {
        if (!submitted(paper) && expired(paper, now)) completeLocked(paper, true, now);
    }

    private void requireSubmitted(ExamPaperEntity paper) {
        if (!submitted(paper)) throw conflict(ErrorCode.EXAM_REVIEW_LOCKED);
    }

    /** Caller owns the paper row lock; every grade, wrong record and transition commits together. */
    private void completeLocked(ExamPaperEntity paper, boolean automatic, LocalDateTime now) {
        if (submitted(paper)) return;
        ExamScoringService.ScoringResult result = scoring.score(papers.findItemSnapshots(paper.getId()), mapper.findAnswers(paper.getId()));
        for (ExamScoringService.ItemScore grade : result.items) {
            ExamAnswerEntity answer = grade.answer;
            boolean insert = answer == null;
            if (insert) {
                answer = new ExamAnswerEntity();
                answer.setPaperId(paper.getId()); answer.setPaperItemId(grade.item.getId());
                answer.setAnswerVersion(0); answer.setSavedAt(automatic ? paper.getDueAt() : now);
            }
            answer.setObjectivelyCorrect(grade.correct); answer.setEarnedScore(grade.earnedScore);
            requireOne(insert ? mapper.insertAnswer(answer) : mapper.updateAnswerGrade(answer));
            if (!grade.correct) {
                ExamWrongRecordEntity wrong = new ExamWrongRecordEntity();
                wrong.setUserId(paper.getUserId()); wrong.setPaperId(paper.getId()); wrong.setPaperItemId(grade.item.getId());
                wrong.setOriginalQuestionId(grade.item.getOriginalQuestionId()); wrong.setWrongAnswer(answer.getAnswerContent());
                wrong.setKnowledgePoints(grade.item.getKnowledgePoints()); wrong.setCreatedAt(now);
                requireOne(mapper.insertWrongRecord(wrong));
            }
        }
        paper.setObjectiveScore(result.objectiveScore); paper.setStatus(automatic ? "AUTO_SUBMITTED" : "SUBMITTED");
        paper.setSubmittedAt(automatic ? paper.getDueAt() : now); paper.setUpdatedAt(now);
        requireOne(mapper.completePaper(paper));
        paper.setVersion(paper.getVersion() + 1);
    }

    private SavedAnswerResponse savedResponse(ExamPaperEntity paper, long itemId, ExamAnswerEntity answer, LocalDateTime now) {
        return new SavedAnswerResponse(paper.getId(), itemId, answer,
                readContent(answer == null ? null : answer.getAnswerContent()), paper.getDueAt(), now);
    }

    private JsonNode readContent(String content) {
        if (content == null) return null;
        try { return json.readTree(content); }
        catch (JsonProcessingException ignored) { return TextNode.valueOf(content); }
    }

    private static void requireOne(int changed) {
        if (changed != 1) throw new IllegalStateException("Exam persistence did not affect exactly one row");
    }

    private static BusinessException conflict(ErrorCode code) { return new BusinessException(HttpStatus.CONFLICT, code); }
    private static BusinessException invalidRequest() { return new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR); }

    private static final class SaveOutcome {
        final SavedAnswerResponse answer;
        final ErrorCode error;
        SaveOutcome(SavedAnswerResponse answer, ErrorCode error) { this.answer = answer; this.error = error; }
    }
}

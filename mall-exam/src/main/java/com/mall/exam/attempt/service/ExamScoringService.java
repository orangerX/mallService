package com.mall.exam.attempt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.mall.exam.attempt.model.ExamAnswerEntity;
import com.mall.exam.paper.model.ExamPaperItemSnapshotEntity;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.*;

/** Pure grading against the immutable snapshot; never reads live question-bank answers. */
@Service
public class ExamScoringService {
    private static final Set<String> OBJECTIVE_TYPES = Set.of("DIALOGUE_BLANK", "READING", "VOCABULARY", "GRAMMAR");
    private final ObjectMapper json;

    public ExamScoringService(ObjectMapper json) { this.json = json; }

    public boolean isObjective(String type) { return type != null && OBJECTIVE_TYPES.contains(type); }

    public boolean isSubjective(String type) { return "TRANSLATION".equals(type) || "WRITING".equals(type); }

    /** A single-choice option, including an unanswered null/blank/empty array. */
    public boolean isValidObjectiveAnswer(JsonNode content) {
        if (content == null || content.isNull()) return true;
        if (content.isArray()) {
            return content.isEmpty() || (content.size() == 1 && content.get(0).isTextual()
                    && isValidObjectiveAnswer(content.get(0)));
        }
        if (!content.isTextual()) return false;
        String value = content.asText().strip().toUpperCase(Locale.ROOT);
        return value.isEmpty() || value.matches("[A-D]");
    }

    /** JSON scalar "A", array ["A"], and legacy plain A normalize to the same value. */
    public List<String> normalize(String content) {
        if (content == null || content.isBlank()) return List.of();
        JsonNode node;
        try { node = json.readTree(content); }
        catch (JsonProcessingException ignored) { return List.of(content.strip().toUpperCase(Locale.ROOT)); }
        List<String> result = new ArrayList<>();
        normalizeNode(node, result);
        return result;
    }

    private void normalizeNode(JsonNode node, List<String> result) {
        if (node == null || node.isNull()) return;
        if (node.isArray()) {
            for (JsonNode member : node) normalizeNode(member, result);
        } else if (node.isValueNode()) {
            String value = node.asText().strip().toUpperCase(Locale.ROOT);
            if (!value.isEmpty()) result.add(value);
        } else {
            // Object-shaped content is never a valid choice, even if both sides are objects.
            result.add("INVALID_OBJECT");
        }
    }

    public ScoringResult score(List<ExamPaperItemSnapshotEntity> items, List<ExamAnswerEntity> answers) {
        Map<Long, ExamAnswerEntity> byItem = new HashMap<>();
        for (ExamAnswerEntity answer : answers) {
            if (byItem.put(answer.getPaperItemId(), answer) != null)
                throw new IllegalStateException("Duplicate answer for a paper item");
        }
        BigDecimal possible = BigDecimal.ZERO;
        BigDecimal earned = BigDecimal.ZERO;
        List<ItemScore> grades = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ExamPaperItemSnapshotEntity item : items) {
            if (!isObjective(item.getQuestionType())) continue;
            if (item.getId() == null || !seen.add(item.getId()) || item.getScore() == null || item.getScore().signum() <= 0)
                throw new IllegalStateException("Invalid objective item snapshot");
            List<String> correctAnswer = normalize(item.getCorrectAnswer());
            if (correctAnswer.size() != 1 || !correctAnswer.get(0).matches("[A-D]"))
                throw new IllegalStateException("Invalid objective answer snapshot");
            possible = possible.add(item.getScore());
            ExamAnswerEntity answer = byItem.get(item.getId());
            List<String> supplied = normalize(answer == null ? null : answer.getAnswerContent());
            boolean correct = !supplied.isEmpty() && correctAnswer.equals(supplied);
            BigDecimal score = correct ? item.getScore() : BigDecimal.ZERO;
            earned = earned.add(score);
            grades.add(new ItemScore(item, answer, correct, score));
        }
        if (possible.compareTo(BigDecimal.valueOf(70)) != 0)
            throw new IllegalStateException("Objective snapshot scores must total 70");
        return new ScoringResult(earned, grades);
    }

    public static final class ScoringResult {
        public final BigDecimal objectiveScore;
        public final List<ItemScore> items;
        ScoringResult(BigDecimal objectiveScore, List<ItemScore> items) {
            this.objectiveScore = objectiveScore;
            this.items = Collections.unmodifiableList(items);
        }
    }

    public static final class ItemScore {
        public final ExamPaperItemSnapshotEntity item;
        public final ExamAnswerEntity answer;
        public final boolean correct;
        public final BigDecimal earnedScore;
        ItemScore(ExamPaperItemSnapshotEntity item, ExamAnswerEntity answer, boolean correct, BigDecimal earnedScore) {
            this.item = item;
            this.answer = answer;
            this.correct = correct;
            this.earnedScore = earnedScore;
        }
    }
}

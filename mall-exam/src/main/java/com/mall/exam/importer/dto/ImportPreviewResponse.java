package com.mall.exam.importer.dto;

import java.util.Collections;
import java.util.List;

public class ImportPreviewResponse {

    private final long batchId;
    private final int totalRows;
    private final int validRows;
    private final int duplicateRows;
    private final List<String> errors;
    private final List<Item> items;

    public ImportPreviewResponse(long batchId, int totalRows, int validRows, int duplicateRows,
                                 List<String> errors, List<Item> items) {
        this.batchId = batchId;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.duplicateRows = duplicateRows;
        this.errors = Collections.unmodifiableList(errors);
        this.items = Collections.unmodifiableList(items);
    }

    public long getBatchId() { return batchId; }
    public int getTotalRows() { return totalRows; }
    public int getValidRows() { return validRows; }
    public int getDuplicateRows() { return duplicateRows; }
    public List<String> getErrors() { return errors; }
    public List<Item> getItems() { return items; }

    public static class Item {
        private final int rowNumber;
        private final String questionType;
        private final String stem;
        private final String options;
        private final String correctAnswer;
        private final String explanation;
        private final String referenceAnswer;
        private final String sampleAnswer;
        private final String scoringRubric;
        private final Integer difficulty;
        private final String knowledgePoints;
        private final Group group;
        private final Integer groupSortOrder;
        private final String fingerprint;

        public Item(int rowNumber, String questionType, String stem, String options, String correctAnswer,
                    String explanation, String referenceAnswer, String sampleAnswer, String scoringRubric,
                    Integer difficulty, String knowledgePoints, Group group, Integer groupSortOrder,
                    String fingerprint) {
            this.rowNumber = rowNumber;
            this.questionType = questionType;
            this.stem = stem;
            this.options = options;
            this.correctAnswer = correctAnswer;
            this.explanation = explanation;
            this.referenceAnswer = referenceAnswer;
            this.sampleAnswer = sampleAnswer;
            this.scoringRubric = scoringRubric;
            this.difficulty = difficulty;
            this.knowledgePoints = knowledgePoints;
            this.group = group;
            this.groupSortOrder = groupSortOrder;
            this.fingerprint = fingerprint;
        }

        public int getRowNumber() { return rowNumber; }
        public String getQuestionType() { return questionType; }
        public String getStem() { return stem; }
        public String getOptions() { return options; }
        public String getCorrectAnswer() { return correctAnswer; }
        public String getExplanation() { return explanation; }
        public String getReferenceAnswer() { return referenceAnswer; }
        public String getSampleAnswer() { return sampleAnswer; }
        public String getScoringRubric() { return scoringRubric; }
        public Integer getDifficulty() { return difficulty; }
        public String getKnowledgePoints() { return knowledgePoints; }
        public Group getGroup() { return group; }
        public Integer getGroupSortOrder() { return groupSortOrder; }
        public String getFingerprint() { return fingerprint; }
    }

    public static class Group {
        private final String groupType;
        private final String title;
        private final String instruction;
        private final String content;
        private final String sharedOptions;
        private final String fingerprint;

        public Group(String groupType, String title, String instruction, String content, String sharedOptions,
                     String fingerprint) {
            this.groupType = groupType;
            this.title = title;
            this.instruction = instruction;
            this.content = content;
            this.sharedOptions = sharedOptions;
            this.fingerprint = fingerprint;
        }

        public String getGroupType() { return groupType; }
        public String getTitle() { return title; }
        public String getInstruction() { return instruction; }
        public String getContent() { return content; }
        public String getSharedOptions() { return sharedOptions; }
        public String getFingerprint() { return fingerprint; }
    }
}

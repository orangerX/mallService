package com.mall.exam.blueprint;

public class ExamSectionSpec {

    private final String questionType;
    private final int sectionOrder;
    private final int groupCount;
    private final int answerItemCount;
    private final int sectionScore;
    private final boolean subjective;

    public ExamSectionSpec(String questionType, int sectionOrder, int groupCount, int answerItemCount,
                           int sectionScore, boolean subjective) {
        this.questionType = questionType;
        this.sectionOrder = sectionOrder;
        this.groupCount = groupCount;
        this.answerItemCount = answerItemCount;
        this.sectionScore = sectionScore;
        this.subjective = subjective;
    }

    public String getQuestionType() { return questionType; }
    public int getSectionOrder() { return sectionOrder; }
    public int getGroupCount() { return groupCount; }
    public int getAnswerItemCount() { return answerItemCount; }
    public int getSectionScore() { return sectionScore; }
    public boolean isSubjective() { return subjective; }
}

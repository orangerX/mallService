package com.mall.exam.question.service;

public class QuestionReviewCommand {

    private boolean approved;
    private String reviewNote;

    public QuestionReviewCommand() {
    }

    public QuestionReviewCommand(boolean approved, String reviewNote) {
        this.approved = approved;
        this.reviewNote = reviewNote;
    }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}

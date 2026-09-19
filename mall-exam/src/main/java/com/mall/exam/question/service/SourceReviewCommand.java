package com.mall.exam.question.service;

public class SourceReviewCommand {

    private boolean approved;
    private String copyrightStatus;
    private String reviewNote;

    public SourceReviewCommand() {
    }

    public SourceReviewCommand(boolean approved, String reviewNote) {
        this(approved, null, reviewNote);
    }

    public SourceReviewCommand(boolean approved, String copyrightStatus, String reviewNote) {
        this.approved = approved;
        this.copyrightStatus = copyrightStatus;
        this.reviewNote = reviewNote;
    }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getCopyrightStatus() { return copyrightStatus; }
    public void setCopyrightStatus(String copyrightStatus) { this.copyrightStatus = copyrightStatus; }
    public String getReviewNote() { return reviewNote; }
    public void setReviewNote(String reviewNote) { this.reviewNote = reviewNote; }
}

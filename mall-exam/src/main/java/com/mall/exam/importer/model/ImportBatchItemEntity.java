package com.mall.exam.importer.model;

public class ImportBatchItemEntity {
    private Long id; private Long batchId; private Integer rowNumber; private String payload;
    private String questionFingerprint; private String groupFingerprint; private String status;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public Long getBatchId() { return batchId; } public void setBatchId(Long batchId) { this.batchId = batchId; }
    public Integer getRowNumber() { return rowNumber; } public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getPayload() { return payload; } public void setPayload(String payload) { this.payload = payload; }
    public String getQuestionFingerprint() { return questionFingerprint; } public void setQuestionFingerprint(String questionFingerprint) { this.questionFingerprint = questionFingerprint; }
    public String getGroupFingerprint() { return groupFingerprint; } public void setGroupFingerprint(String groupFingerprint) { this.groupFingerprint = groupFingerprint; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
}

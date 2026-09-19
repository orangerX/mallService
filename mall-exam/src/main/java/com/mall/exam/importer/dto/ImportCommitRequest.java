package com.mall.exam.importer.dto;

public class ImportCommitRequest {

    @javax.validation.constraints.NotNull
    @javax.validation.constraints.Min(1)
    private Long batchId;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
}

package com.mall.exam.importer.model;

public class ImportBatchEntity {
    private Long id; private String fileName; private String fileFormat; private Long sourceId; private Long importedBy;
    private String status; private Integer totalRows; private Integer successRows; private Integer failedRows; private Integer duplicateRows;
    private String structureErrors; private String duplicateWarnings;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; } public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileFormat() { return fileFormat; } public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public Long getSourceId() { return sourceId; } public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Long getImportedBy() { return importedBy; } public void setImportedBy(Long importedBy) { this.importedBy = importedBy; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public Integer getTotalRows() { return totalRows; } public void setTotalRows(Integer totalRows) { this.totalRows = totalRows; }
    public Integer getSuccessRows() { return successRows; } public void setSuccessRows(Integer successRows) { this.successRows = successRows; }
    public Integer getFailedRows() { return failedRows; } public void setFailedRows(Integer failedRows) { this.failedRows = failedRows; }
    public Integer getDuplicateRows() { return duplicateRows; } public void setDuplicateRows(Integer duplicateRows) { this.duplicateRows = duplicateRows; }
    public String getStructureErrors() { return structureErrors; } public void setStructureErrors(String structureErrors) { this.structureErrors = structureErrors; }
    public String getDuplicateWarnings() { return duplicateWarnings; } public void setDuplicateWarnings(String duplicateWarnings) { this.duplicateWarnings = duplicateWarnings; }
}

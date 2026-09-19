package com.mall.exam.paper.model;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ExamPaperEntity {
    private Long id;
    private Long userId;
    private Long blueprintId;
    private String paperNo;
    private String blueprintVersionSnapshot;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime dueAt;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BigDecimal objectiveScore;
    private BigDecimal selfScoreTotal;
    private Integer translationSelfScore;
    private Integer writingSelfScore;
    private Integer version;
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;}
    public void setUserId(Long userId){this.userId=userId;}
    public Long getBlueprintId(){return blueprintId;}
    public void setBlueprintId(Long blueprintId){this.blueprintId=blueprintId;}
    public String getPaperNo(){return paperNo;}
    public void setPaperNo(String paperNo){this.paperNo=paperNo;}
    public String getBlueprintVersionSnapshot(){return blueprintVersionSnapshot;}
    public void setBlueprintVersionSnapshot(String blueprintVersionSnapshot){this.blueprintVersionSnapshot=blueprintVersionSnapshot;}
    public String getStatus(){return status;}
    public void setStatus(String status){this.status=status;}
    public LocalDateTime getStartedAt(){return startedAt;}
    public void setStartedAt(LocalDateTime startedAt){this.startedAt=startedAt;}
    public LocalDateTime getDueAt(){return dueAt;}
    public void setDueAt(LocalDateTime dueAt){this.dueAt=dueAt;}
    public LocalDateTime getSubmittedAt(){return submittedAt;}
    public void setSubmittedAt(LocalDateTime submittedAt){this.submittedAt=submittedAt;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt=updatedAt;}
    public BigDecimal getObjectiveScore(){return objectiveScore;}
    public void setObjectiveScore(BigDecimal objectiveScore){this.objectiveScore=objectiveScore;}
    public BigDecimal getSelfScoreTotal(){return selfScoreTotal;}
    public void setSelfScoreTotal(BigDecimal selfScoreTotal){this.selfScoreTotal=selfScoreTotal;}
    public Integer getTranslationSelfScore(){return translationSelfScore;}
    public void setTranslationSelfScore(Integer translationSelfScore){this.translationSelfScore=translationSelfScore;}
    public Integer getWritingSelfScore(){return writingSelfScore;}
    public void setWritingSelfScore(Integer writingSelfScore){this.writingSelfScore=writingSelfScore;}
    public Integer getVersion(){return version;}
    public void setVersion(Integer version){this.version=version;}
}

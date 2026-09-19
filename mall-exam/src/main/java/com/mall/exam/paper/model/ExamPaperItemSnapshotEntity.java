package com.mall.exam.paper.model;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ExamPaperItemSnapshotEntity {
    private Long id;
    private Long paperId;
    private Long paperGroupSnapshotId;
    private Long originalQuestionId;
    private Integer sectionOrder;
    private Integer itemOrder;
    private String questionType;
    private String stem;
    private String options;
    private String correctAnswer;
    private String explanation;
    private String referenceAnswer;
    private String sampleAnswer;
    private String scoringRubric;
    private String knowledgePoints;
    private BigDecimal score;
    private LocalDateTime createdAt;
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getPaperId(){return paperId;}
    public void setPaperId(Long paperId){this.paperId=paperId;}
    public Long getPaperGroupSnapshotId(){return paperGroupSnapshotId;}
    public void setPaperGroupSnapshotId(Long paperGroupSnapshotId){this.paperGroupSnapshotId=paperGroupSnapshotId;}
    public Long getOriginalQuestionId(){return originalQuestionId;}
    public void setOriginalQuestionId(Long originalQuestionId){this.originalQuestionId=originalQuestionId;}
    public Integer getSectionOrder(){return sectionOrder;}
    public void setSectionOrder(Integer sectionOrder){this.sectionOrder=sectionOrder;}
    public Integer getItemOrder(){return itemOrder;}
    public void setItemOrder(Integer itemOrder){this.itemOrder=itemOrder;}
    public String getQuestionType(){return questionType;}
    public void setQuestionType(String questionType){this.questionType=questionType;}
    public String getStem(){return stem;}
    public void setStem(String stem){this.stem=stem;}
    public String getOptions(){return options;}
    public void setOptions(String options){this.options=options;}
    public String getCorrectAnswer(){return correctAnswer;}
    public void setCorrectAnswer(String correctAnswer){this.correctAnswer=correctAnswer;}
    public String getExplanation(){return explanation;}
    public void setExplanation(String explanation){this.explanation=explanation;}
    public String getReferenceAnswer(){return referenceAnswer;}
    public void setReferenceAnswer(String referenceAnswer){this.referenceAnswer=referenceAnswer;}
    public String getSampleAnswer(){return sampleAnswer;}
    public void setSampleAnswer(String sampleAnswer){this.sampleAnswer=sampleAnswer;}
    public String getScoringRubric(){return scoringRubric;}
    public void setScoringRubric(String scoringRubric){this.scoringRubric=scoringRubric;}
    public String getKnowledgePoints(){return knowledgePoints;}
    public void setKnowledgePoints(String knowledgePoints){this.knowledgePoints=knowledgePoints;}
    public BigDecimal getScore(){return score;}
    public void setScore(BigDecimal score){this.score=score;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}

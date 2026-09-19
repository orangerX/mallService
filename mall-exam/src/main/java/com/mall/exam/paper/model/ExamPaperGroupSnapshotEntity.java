package com.mall.exam.paper.model;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public class ExamPaperGroupSnapshotEntity {
    private Long id;
    private Long paperId;
    private Long originalGroupId;
    private Integer sectionOrder;
    private Integer groupOrder;
    private String title;
    private String instruction;
    private String content;
    private String sharedOptions;
    private LocalDateTime createdAt;
    public Long getId(){return id;}
    public void setId(Long id){this.id=id;}
    public Long getPaperId(){return paperId;}
    public void setPaperId(Long paperId){this.paperId=paperId;}
    public Long getOriginalGroupId(){return originalGroupId;}
    public void setOriginalGroupId(Long originalGroupId){this.originalGroupId=originalGroupId;}
    public Integer getSectionOrder(){return sectionOrder;}
    public void setSectionOrder(Integer sectionOrder){this.sectionOrder=sectionOrder;}
    public Integer getGroupOrder(){return groupOrder;}
    public void setGroupOrder(Integer groupOrder){this.groupOrder=groupOrder;}
    public String getTitle(){return title;}
    public void setTitle(String title){this.title=title;}
    public String getInstruction(){return instruction;}
    public void setInstruction(String instruction){this.instruction=instruction;}
    public String getContent(){return content;}
    public void setContent(String content){this.content=content;}
    public String getSharedOptions(){return sharedOptions;}
    public void setSharedOptions(String sharedOptions){this.sharedOptions=sharedOptions;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt=createdAt;}
}

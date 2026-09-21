package com.mall.exam.paper.template.model;

import java.time.LocalDateTime;

/** Read-only frozen template data; never expose this model as a student response. */
public final class FixedPaperTemplateGroup {
    private final Long id;
    private final Long fixedPaperId;
    private final Long originalGroupId;
    private final Long sourceId;
    private final String contentFingerprint;
    private final Integer sectionOrder;
    private final Integer groupOrder;
    private final String groupType;
    private final String title;
    private final String instruction;
    private final String content;
    private final String sharedOptions;
    private final LocalDateTime createdAt;

    public FixedPaperTemplateGroup(
            Long id,
            Long fixedPaperId,
            Long originalGroupId,
            Long sourceId,
            String contentFingerprint,
            Integer sectionOrder,
            Integer groupOrder,
            String groupType,
            String title,
            String instruction,
            String content,
            String sharedOptions,
            LocalDateTime createdAt) {
        this.id = id;
        this.fixedPaperId = fixedPaperId;
        this.originalGroupId = originalGroupId;
        this.sourceId = sourceId;
        this.contentFingerprint = contentFingerprint;
        this.sectionOrder = sectionOrder;
        this.groupOrder = groupOrder;
        this.groupType = groupType;
        this.title = title;
        this.instruction = instruction;
        this.content = content;
        this.sharedOptions = sharedOptions;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }

    public Long getFixedPaperId() { return fixedPaperId; }

    public Long getOriginalGroupId() { return originalGroupId; }

    public Long getSourceId() { return sourceId; }

    public String getContentFingerprint() { return contentFingerprint; }

    public Integer getSectionOrder() { return sectionOrder; }

    public Integer getGroupOrder() { return groupOrder; }

    public String getGroupType() { return groupType; }

    public String getTitle() { return title; }

    public String getInstruction() { return instruction; }

    public String getContent() { return content; }

    public String getSharedOptions() { return sharedOptions; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

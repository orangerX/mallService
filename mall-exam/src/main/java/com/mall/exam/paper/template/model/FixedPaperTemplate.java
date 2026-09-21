package com.mall.exam.paper.template.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Read-only frozen template data; never expose this model as a student response. */
public final class FixedPaperTemplate {
    private final Long id;
    private final String paperCode;
    private final Integer version;
    private final String title;
    private final Long blueprintId;
    private final String blueprintCode;
    private final String blueprintVersionSnapshot;
    private final Integer durationMinutes;
    private final BigDecimal totalScore;
    private final Long sourceId;
    private final String sourceDeclaration;
    private final String contentFingerprint;
    private final Integer displayOrder;
    private final String status;
    private final LocalDateTime publishedAt;
    private final LocalDateTime createdAt;
    private final List<FixedPaperTemplateGroup> groups;
    private final List<FixedPaperTemplateItem> items;

    public FixedPaperTemplate(
            Long id,
            String paperCode,
            Integer version,
            String title,
            Long blueprintId,
            String blueprintCode,
            String blueprintVersionSnapshot,
            Integer durationMinutes,
            BigDecimal totalScore,
            Long sourceId,
            String sourceDeclaration,
            String contentFingerprint,
            Integer displayOrder,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime createdAt) {
        this(id, paperCode, version, title, blueprintId, blueprintCode, blueprintVersionSnapshot, durationMinutes, totalScore, sourceId, sourceDeclaration, contentFingerprint, displayOrder, status, publishedAt, createdAt, List.of(), List.of());
    }

    private FixedPaperTemplate(
            Long id,
            String paperCode,
            Integer version,
            String title,
            Long blueprintId,
            String blueprintCode,
            String blueprintVersionSnapshot,
            Integer durationMinutes,
            BigDecimal totalScore,
            Long sourceId,
            String sourceDeclaration,
            String contentFingerprint,
            Integer displayOrder,
            String status,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            List<FixedPaperTemplateGroup> groups,
            List<FixedPaperTemplateItem> items) {
        this.id = id;
        this.paperCode = paperCode;
        this.version = version;
        this.title = title;
        this.blueprintId = blueprintId;
        this.blueprintCode = blueprintCode;
        this.blueprintVersionSnapshot = blueprintVersionSnapshot;
        this.durationMinutes = durationMinutes;
        this.totalScore = totalScore;
        this.sourceId = sourceId;
        this.sourceDeclaration = sourceDeclaration;
        this.contentFingerprint = contentFingerprint;
        this.displayOrder = displayOrder;
        this.status = status;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.groups = List.copyOf(groups);
        this.items = List.copyOf(items);
    }

    public Long getId() { return id; }

    public String getPaperCode() { return paperCode; }

    public Integer getVersion() { return version; }

    public String getTitle() { return title; }

    public Long getBlueprintId() { return blueprintId; }

    public String getBlueprintCode() { return blueprintCode; }

    public String getBlueprintVersionSnapshot() { return blueprintVersionSnapshot; }

    public Integer getDurationMinutes() { return durationMinutes; }

    public BigDecimal getTotalScore() { return totalScore; }

    public Long getSourceId() { return sourceId; }

    public String getSourceDeclaration() { return sourceDeclaration; }

    public String getContentFingerprint() { return contentFingerprint; }

    public Integer getDisplayOrder() { return displayOrder; }

    public String getStatus() { return status; }

    public LocalDateTime getPublishedAt() { return publishedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public FixedPaperTemplate withSnapshots(List<FixedPaperTemplateGroup> groups,
                                           List<FixedPaperTemplateItem> items) {
        return new FixedPaperTemplate(id, paperCode, version, title, blueprintId, blueprintCode, blueprintVersionSnapshot, durationMinutes, totalScore, sourceId, sourceDeclaration, contentFingerprint, displayOrder, status, publishedAt, createdAt, groups, items);
    }

    @JsonIgnore
    public List<FixedPaperTemplateGroup> getGroups() { return groups; }

    @JsonIgnore
    public List<FixedPaperTemplateItem> getItems() { return items; }
}

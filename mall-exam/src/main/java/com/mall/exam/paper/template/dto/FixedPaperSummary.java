package com.mall.exam.paper.template.dto;

import java.math.BigDecimal;

/** Answer-free template metadata for service consumers. */
public final class FixedPaperSummary {
    private final Long id;
    private final String paperCode;
    private final Integer version;
    private final String title;
    private final Long blueprintId;
    private final String blueprintCode;
    private final String blueprintVersionSnapshot;
    private final Integer durationMinutes;
    private final BigDecimal totalScore;
    private final String sourceDeclaration;
    private final Integer displayOrder;
    private final Integer groupCount;
    private final Integer itemCount;

    public FixedPaperSummary(
            Long id,
            String paperCode,
            Integer version,
            String title,
            Long blueprintId,
            String blueprintCode,
            String blueprintVersionSnapshot,
            Integer durationMinutes,
            BigDecimal totalScore,
            String sourceDeclaration,
            Integer displayOrder,
            Integer groupCount,
            Integer itemCount) {
        this.id = id;
        this.paperCode = paperCode;
        this.version = version;
        this.title = title;
        this.blueprintId = blueprintId;
        this.blueprintCode = blueprintCode;
        this.blueprintVersionSnapshot = blueprintVersionSnapshot;
        this.durationMinutes = durationMinutes;
        this.totalScore = totalScore;
        this.sourceDeclaration = sourceDeclaration;
        this.displayOrder = displayOrder;
        this.groupCount = groupCount;
        this.itemCount = itemCount;
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

    public String getSourceDeclaration() { return sourceDeclaration; }

    public Integer getDisplayOrder() { return displayOrder; }

    public Integer getGroupCount() { return groupCount; }

    public Integer getItemCount() { return itemCount; }
}

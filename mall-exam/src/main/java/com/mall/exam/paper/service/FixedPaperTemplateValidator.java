package com.mall.exam.paper.service;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.blueprint.ExamBlueprintDefinition;
import com.mall.exam.blueprint.ExamSectionSpec;
import com.mall.exam.paper.mapper.ExamPaperMapper;
import com.mall.exam.paper.template.model.FixedPaperTemplate;
import com.mall.exam.paper.template.model.FixedPaperTemplateGroup;
import com.mall.exam.paper.template.model.FixedPaperTemplateItem;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Rejects malformed publications before the first learner-owned row is inserted. */
final class FixedPaperTemplateValidator {
    private FixedPaperTemplateValidator() { }

    static void validate(FixedPaperTemplate template, ExamBlueprintDefinition blueprint,
                         ExamPaperMapper.BlueprintRow enabledBlueprint) {
        require(template.getId() != null && template.getSourceId() != null
                && template.getVersion() != null && template.getVersion() > 0
                && "PUBLISHED".equals(template.getStatus()) && template.getPublishedAt() != null
                && blueprint.getCode().equals(template.getBlueprintCode())
                && enabledBlueprint != null && enabledBlueprint.id.equals(template.getBlueprintId())
                && enabledBlueprint.version.equals(template.getBlueprintVersionSnapshot())
                && Integer.valueOf(blueprint.getDurationMinutes()).equals(template.getDurationMinutes())
                && template.getTotalScore() != null
                && BigDecimal.valueOf(blueprint.getTotalScore()).compareTo(template.getTotalScore()) == 0);

        Map<Long, FixedPaperTemplateGroup> groupsById = new HashMap<>();
        Set<Long> originalGroupIds = new HashSet<>();
        Set<Long> itemIds = new HashSet<>();
        Set<Long> originalQuestionIds = new HashSet<>();
        int groupCount = 0;
        int itemCount = 0;
        for (ExamSectionSpec section : blueprint.getSections()) {
            List<FixedPaperTemplateGroup> groups = template.getGroups().stream()
                    .filter(group -> Integer.valueOf(section.getSectionOrder()).equals(group.getSectionOrder()))
                    .collect(Collectors.toList());
            List<FixedPaperTemplateItem> items = template.getItems().stream()
                    .filter(item -> Integer.valueOf(section.getSectionOrder()).equals(item.getSectionOrder()))
                    .collect(Collectors.toList());
            require(groups.size() == section.getGroupCount() && items.size() == section.getAnswerItemCount());
            groupCount += groups.size();
            itemCount += items.size();
            Map<Long, Integer> memberCounts = new HashMap<>();
            for (int index = 0; index < groups.size(); index++) {
                FixedPaperTemplateGroup group = groups.get(index);
                require(group.getId() != null && groupsById.put(group.getId(), group) == null
                        && template.getId().equals(group.getFixedPaperId())
                        && template.getSourceId().equals(group.getSourceId())
                        && group.getOriginalGroupId() != null && originalGroupIds.add(group.getOriginalGroupId())
                        && Integer.valueOf(index + 1).equals(group.getGroupOrder())
                        && section.getQuestionType().equals(group.getGroupType()) && hasText(group.getContent()));
                if ("DIALOGUE".equals(group.getGroupType())) require(hasText(group.getSharedOptions()));
            }
            String type = "DIALOGUE".equals(section.getQuestionType()) ? "DIALOGUE_BLANK" : section.getQuestionType();
            BigDecimal score = BigDecimal.valueOf(section.getSectionScore())
                    .divide(BigDecimal.valueOf(section.getAnswerItemCount()));
            int lastGroupOrder = 0;
            for (int index = 0; index < items.size(); index++) {
                FixedPaperTemplateItem item = items.get(index);
                require(item.getId() != null && itemIds.add(item.getId())
                        && template.getId().equals(item.getFixedPaperId())
                        && template.getSourceId().equals(item.getSourceId())
                        && item.getOriginalQuestionId() != null && originalQuestionIds.add(item.getOriginalQuestionId())
                        && Integer.valueOf(index + 1).equals(item.getItemOrder())
                        && type.equals(item.getQuestionType()) && hasText(item.getStem())
                        && item.getScore() != null && score.compareTo(item.getScore()) == 0);
                if (section.getGroupCount() == 0) {
                    require(item.getFixedPaperGroupId() == null);
                } else {
                    FixedPaperTemplateGroup group = groupsById.get(item.getFixedPaperGroupId());
                    require(group != null && group.getSectionOrder().equals(item.getSectionOrder())
                            && group.getGroupOrder() >= lastGroupOrder);
                    lastGroupOrder = group.getGroupOrder();
                    memberCounts.merge(group.getId(), 1, Integer::sum);
                }
                if (section.isSubjective()) {
                    require(hasText(item.getScoringRubric()));
                    require("TRANSLATION".equals(type) ? hasText(item.getReferenceAnswer()) : hasText(item.getSampleAnswer()));
                } else {
                    require(hasText(item.getCorrectAnswer()) && hasText(item.getExplanation()));
                    if (!"DIALOGUE_BLANK".equals(type)) require(hasText(item.getOptions()));
                }
            }
            for (FixedPaperTemplateGroup group : groups) {
                int members = memberCounts.getOrDefault(group.getId(), 0);
                require("READING".equals(group.getGroupType()) ? members == 5 : members >= 1 && members <= 8);
            }
        }
        require(groupCount == template.getGroups().size() && itemCount == template.getItems().size());
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank() && !"null".equals(value.trim());
    }

    private static void require(boolean valid) {
        if (!valid) throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.EXAM_IMPORT_INVALID);
    }
}

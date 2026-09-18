package com.mall.exam.blueprint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExamBlueprintDefinitionTest {

    @Test
    void degreeEnglishBlueprintHasApprovedStructure() {
        ExamBlueprintDefinition blueprint = ExamBlueprintDefinition.degreeEnglish2016V2();

        assertEquals("DEGREE_ENGLISH_2016_V2", blueprint.getCode());
        assertEquals(120, blueprint.getDurationMinutes());
        assertEquals(100, blueprint.getTotalScore());
        assertEquals(52, blueprint.getSections().stream().mapToInt(ExamSectionSpec::getAnswerItemCount).sum());
        assertEquals(70, blueprint.getSections().stream().filter(section -> !section.isSubjective())
                .mapToInt(ExamSectionSpec::getSectionScore).sum());
    }
}

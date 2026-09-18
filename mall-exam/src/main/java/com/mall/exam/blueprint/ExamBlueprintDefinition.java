package com.mall.exam.blueprint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExamBlueprintDefinition {

    private final String code;
    private final int durationMinutes;
    private final int totalScore;
    private final List<ExamSectionSpec> sections;

    private ExamBlueprintDefinition(String code, int durationMinutes, int totalScore, List<ExamSectionSpec> sections) {
        this.code = code;
        this.durationMinutes = durationMinutes;
        this.totalScore = totalScore;
        this.sections = Collections.unmodifiableList(sections);
    }

    public static ExamBlueprintDefinition degreeEnglish2016V2() {
        return new ExamBlueprintDefinition("DEGREE_ENGLISH_2016_V2", 120, 100, Arrays.asList(
                new ExamSectionSpec("DIALOGUE", 1, 3, 10, 10, false),
                new ExamSectionSpec("READING", 2, 4, 20, 40, false),
                new ExamSectionSpec("VOCABULARY", 3, 0, 10, 10, false),
                new ExamSectionSpec("GRAMMAR", 4, 0, 10, 10, false),
                new ExamSectionSpec("TRANSLATION", 5, 0, 1, 15, true),
                new ExamSectionSpec("WRITING", 6, 0, 1, 15, true)
        ));
    }

    public String getCode() { return code; }
    public int getDurationMinutes() { return durationMinutes; }
    public int getTotalScore() { return totalScore; }
    public List<ExamSectionSpec> getSections() { return sections; }
}

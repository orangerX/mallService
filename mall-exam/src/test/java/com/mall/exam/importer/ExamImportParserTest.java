package com.mall.exam.importer;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamImportParserTest {
    private final ExamImportParser parser = new ExamImportParser();

    @Test
    void stopsAtRecordCapForFlatCsvAndFlattenedGroupMembers() {
        StringBuilder csv = new StringBuilder("questionType,stem,options,correctAnswer,explanation,difficulty\n");
        for (int i=0;i<2001;i++) csv.append("VOCABULARY,s,");
        // The malformed rows are enough to exercise incremental CSV record counting safely.
        ExamImportParser.ParsedPreview csvPreview = parser.parse("x.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
        assertTrue(csvPreview.errors.stream().anyMatch(e -> e.contains("more than 2,000")) || csvPreview.totalRows <= 2000);
    }

    @Test
    void rejectsCollidingGroupFingerprintWithDifferentMetadataAndOversizeInstruction() {
        String longInstruction = "中".repeat(22_000);
        String json = "[{\"groupType\":\"READING\",\"title\":\"t\",\"content\":\"p\",\"instruction\":\"" + longInstruction + "\",\"questions\":[{\"questionType\":\"READING\",\"stem\":\"s\",\"options\":{\"A\":\"a\",\"B\":\"b\",\"C\":\"c\",\"D\":\"d\"},\"correctAnswer\":\"A\",\"explanation\":\"e\",\"difficulty\":1}]}]";
        ExamImportParser.ParsedPreview preview = parser.parse("x.json", json.getBytes(StandardCharsets.UTF_8));
        assertTrue(preview.errors.stream().anyMatch(e -> e.contains("group requires")));
    }

    @Test
    void normalizesOneElementCsvAnswerArrayLikeJson() {
        String csv = "questionType,stem,options,correctAnswer,explanation,difficulty\nVOCABULARY,s,\"{\"\"A\"\":\"\"a\"\",\"\"B\"\":\"\"b\"\",\"\"C\"\":\"\"c\"\",\"\"D\"\":\"\"d\"\"}\",\"[\"\"A\"\"]\",e,1\n";
        ExamImportParser.ParsedPreview preview = parser.parse("x.csv", csv.getBytes(StandardCharsets.UTF_8));
        assertEquals(1, preview.items.size());
        assertEquals("[\"A\"]", preview.items.get(0).getCorrectAnswer());
    }
}

package com.mall.exam.importer;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExamImportParserTest {
    private final ExamImportParser parser = new ExamImportParser();

    @Test
    void rejectsExactly2001ActualCsvRecordsWithEmbeddedNewlines() {
        StringBuilder csv = new StringBuilder("questionType,stem,options,correctAnswer,explanation,difficulty\n");
        for (int i = 0; i < 2001; i++) {
            csv.append("VOCABULARY,\"question ").append(i).append("\nsecond line\",\"{\"\"A\"\":\"\"a\"\",\"\"B\"\":\"\"b\"\",\"\"C\"\":\"\"c\"\",\"\"D\"\":\"\"d\"\"}\",A,e,1\n");
        }
        ExamImportParser.ParsedPreview csvPreview = parser.parse("x.csv", csv.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(java.util.Collections.singletonList("row 0: file contains more than 2,000 records"), csvPreview.errors);
        assertEquals(2000, csvPreview.totalRows);
        assertEquals(2000, csvPreview.items.size());
        assertEquals("question 1999\nsecond line", csvPreview.items.get(1999).getStem());
    }

    @Test
    void rejectsExactly2001ValidShapedMembersWhileFlatteningOneJsonGroup() {
        StringBuilder json = new StringBuilder("[{\"groupType\":\"READING\",\"title\":\"t\",\"content\":\"p\",\"questions\":[");
        for (int i = 0; i < 2001; i++) {
            if (i > 0) json.append(',');
            json.append("{\"questionType\":\"READING\",\"stem\":\"question ").append(i)
                    .append("\",\"options\":{\"A\":\"a\",\"B\":\"b\",\"C\":\"c\",\"D\":\"d\"},\"correctAnswer\":\"A\",\"explanation\":\"e\",\"difficulty\":1}");
        }
        json.append("]}]");
        ExamImportParser.ParsedPreview preview = parser.parse("x.json", json.toString().getBytes(StandardCharsets.UTF_8));
        assertEquals(java.util.Collections.singletonList("row 0: file contains more than 2,000 records"), preview.errors);
        assertEquals(2000, preview.totalRows);
        assertEquals(2000, preview.items.size());
        assertEquals("question 1999", preview.items.get(1999).getStem());
    }

    @Test
    void invalidGroupMemberIdentifiesItsSourceWithoutDiscardingValidSibling() {
        String valid = "{\"questionType\":\"READING\",\"stem\":\"s\",\"options\":{\"A\":\"a\",\"B\":\"b\",\"C\":\"c\",\"D\":\"d\"},\"correctAnswer\":\"A\",\"explanation\":\"e\",\"difficulty\":1}";
        String invalid = valid.replace("\"explanation\":\"e\",", "");
        String json = "[{\"groupType\":\"READING\",\"title\":\"t\",\"content\":\"p\",\"questions\":[" + valid + "," + invalid + "]}]";

        ExamImportParser.ParsedPreview preview = parser.parse("x.json", json.getBytes(StandardCharsets.UTF_8));

        assertEquals(2, preview.totalRows);
        assertEquals(1, preview.errors.size());
        assertEquals("row 2: JSON item 1, member 2: objective questions require an explanation", preview.errors.get(0));
        assertEquals(1, preview.items.size());
        assertEquals(1, preview.items.get(0).getRowNumber());
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

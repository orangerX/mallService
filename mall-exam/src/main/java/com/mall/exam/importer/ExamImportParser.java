package com.mall.exam.importer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mall.exam.importer.dto.ImportPreviewResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Parses only the portable import representation; it deliberately performs no database writes. */
public class ExamImportParser {

    static final int MAX_BYTES = 5 * 1024 * 1024;
    static final int MAX_ROWS = 2_000;
    private static final Set<String> QUESTION_TYPES = new HashSet<>(Arrays.asList(
            "DIALOGUE_BLANK", "READING", "VOCABULARY", "GRAMMAR", "TRANSLATION", "WRITING"));
    private static final Set<String> OBJECTIVE_TYPES = new HashSet<>(Arrays.asList(
            "DIALOGUE_BLANK", "READING", "VOCABULARY", "GRAMMAR"));
    private final ObjectMapper objectMapper;

    public ExamImportParser() {
        this(new ObjectMapper());
    }

    ExamImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    ParsedPreview parse(String filename, byte[] bytes) {
        List<String> errors = new ArrayList<>();
        String format = format(filename, errors);
        if (bytes == null || bytes.length == 0) {
            errors.add("row 0: file is empty");
        } else if (bytes.length > MAX_BYTES) {
            errors.add("row 0: file exceeds 5 MiB");
        }
        if (!errors.isEmpty()) {
            return new ParsedPreview(0, Collections.emptyList(), errors);
        }

        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException error) {
            return new ParsedPreview(0, Collections.emptyList(),
                    Collections.singletonList("row 0: file must be UTF-8"));
        }
        if (!text.isEmpty() && text.charAt(0) == '\ufeff') text = text.substring(1);

        List<Row> rows = "json".equals(format) ? jsonRows(text, errors) : csvRows(text, errors);
        if (rows.size() > MAX_ROWS) {
            errors.add("row 0: file contains more than 2,000 records");
            return new ParsedPreview(rows.size(), Collections.emptyList(), errors);
        }
        List<ImportPreviewResponse.Item> items = new ArrayList<>();
        for (Row row : rows) {
            ImportPreviewResponse.Item item = item(row, errors);
            if (item != null) items.add(item);
        }
        validateGroups(items, errors);
        Set<Integer> invalidRows = invalidRows(errors);
        if (!invalidRows.isEmpty()) {
            items.removeIf(item -> invalidRows.contains(item.getRowNumber()));
        }
        return new ParsedPreview(rows.size(), items, errors);
    }

    private List<Row> jsonRows(String text, List<String> errors) {
        try {
            JsonNode root = objectMapper.readTree(text);
            List<Row> rows = new ArrayList<>();
            JsonNode candidates = root != null && root.isObject() && root.has("items") ? root.get("items") : root;
            if (candidates == null || (!candidates.isArray() && !candidates.isObject())) {
                errors.add("row 0: JSON must be an array or an object with an items array");
                return rows;
            }
            if (candidates.isObject()) candidates = objectMapper.createArrayNode().add(candidates);
            int rowNumber = 0;
            for (JsonNode candidate : candidates) {
                rowNumber++;
                if (!candidate.isObject()) {
                    errors.add("row " + rowNumber + ": record must be an object");
                    continue;
                }
                JsonNode questions = candidate.get("questions");
                if (questions != null) {
                    if (!questions.isArray() || questions.isEmpty()) {
                        errors.add("row " + rowNumber + ": group questions must be a non-empty array");
                        continue;
                    }
                    for (JsonNode question : questions) {
                        if (!question.isObject()) {
                            errors.add("row " + rowNumber + ": group member must be an object");
                            continue;
                        }
                        ObjectNode copy = ((ObjectNode) question).deepCopy();
                        ObjectNode group = ((ObjectNode) candidate).deepCopy();
                        group.remove("questions");
                        copy.set("group", group);
                        rows.add(new Row(rowNumber, copy));
                    }
                } else {
                    rows.add(new Row(rowNumber, candidate));
                }
            }
            return rows;
        } catch (IOException error) {
            errors.add("row 0: malformed JSON");
            return Collections.emptyList();
        }
    }

    private List<Row> csvRows(String text, List<String> errors) {
        List<Row> rows = new ArrayList<>();
        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build()
                .parse(new StringReader(text))) {
            if (parser.getHeaderMap().isEmpty()) {
                errors.add("row 0: CSV header is required");
                return rows;
            }
            for (CSVRecord record : parser) {
                ObjectNode node = objectMapper.createObjectNode();
                for (String header : parser.getHeaderMap().keySet()) node.put(header, record.get(header));
                normalizeCsvJson(node, "options");
                normalizeCsvJson(node, "sharedOptions");
                normalizeCsvJson(node, "scoringRubric");
                normalizeCsvJson(node, "knowledgePoints");
                rows.add(new Row((int) record.getRecordNumber() + 1, node));
            }
        } catch (IOException | IllegalArgumentException error) {
            errors.add("row 0: malformed CSV");
        }
        return rows;
    }

    private void normalizeCsvJson(ObjectNode node, String field) {
        JsonNode value = node.get(field);
        if (value != null && value.isTextual() && hasText(value.asText())) {
            try {
                node.set(field, objectMapper.readTree(value.asText()));
            } catch (IOException ignored) {
                // Validation will attach a deterministic row error later.
            }
        }
    }

    private ImportPreviewResponse.Item item(Row row, List<String> errors) {
        JsonNode node = row.node;
        String type = upper(text(node, "questionType"));
        if (!QUESTION_TYPES.contains(type)) return invalid(row, errors, "unsupported questionType");
        String stem = text(node, "stem");
        if (!within(stem, 1, 10_000)) return invalid(row, errors, "stem must contain 1 to 10,000 characters");
        Integer difficulty = integer(node, "difficulty");
        if (difficulty == null || difficulty < 1 || difficulty > 5) return invalid(row, errors, "difficulty must be 1 to 5");

        ImportPreviewResponse.Group group = group(row, node, errors);
        if (("DIALOGUE_BLANK".equals(type) && (group == null || !"DIALOGUE".equals(group.getGroupType())))
                || ("READING".equals(type) && (group == null || !"READING".equals(group.getGroupType())))) {
            return invalid(row, errors, type + " must belong to its matching question group");
        }
        if (("TRANSLATION".equals(type) || "WRITING".equals(type)) && group != null) {
            return invalid(row, errors, type + " cannot belong to a question group");
        }

        JsonNode optionsNode = node.get("options");
        JsonNode effectiveOptions = optionsNode == null || optionsNode.isNull()
                ? groupOptions(node) : optionsNode;
        String options = json(optionsNode, row, "options", errors);
        String correctAnswer = null;
        String explanation = text(node, "explanation");
        String referenceAnswer = text(node, "referenceAnswer");
        String sampleAnswer = text(node, "sampleAnswer");
        String scoringRubric = json(node.get("scoringRubric"), row, "scoringRubric", errors);
        if (OBJECTIVE_TYPES.contains(type)) {
            if (!optionsAreABCD(effectiveOptions)) return invalid(row, errors, "objective questions require non-empty A-D options");
            String answer = answer(node.get("correctAnswer"));
            if (!"A".equals(answer) && !"B".equals(answer) && !"C".equals(answer) && !"D".equals(answer)) {
                return invalid(row, errors, "objective questions require a correctAnswer from A-D");
            }
            if (!within(explanation, 1, 10_000)) return invalid(row, errors, "objective questions require an explanation");
            correctAnswer = jsonArray(answer);
        } else if ("TRANSLATION".equals(type)) {
            if (!within(referenceAnswer, 1, 20_000) || !hasJsonContent(node.get("scoringRubric"))) {
                return invalid(row, errors, "translation requires referenceAnswer and scoringRubric");
            }
        } else if ("WRITING".equals(type)) {
            if (!within(sampleAnswer, 1, 20_000) || !hasJsonContent(node.get("scoringRubric"))) {
                return invalid(row, errors, "writing requires sampleAnswer and scoringRubric");
            }
        }
        if (optionsNode != null && options == null) return null;
        if (node.get("scoringRubric") != null && scoringRubric == null) return null;
        String knowledgePoints = node.get("knowledgePoints") == null ? "[]" : json(node.get("knowledgePoints"), row, "knowledgePoints", errors);
        if (knowledgePoints == null) return null;
        String groupMaterial = group == null ? "" : group.getContent();
        return new ImportPreviewResponse.Item(row.number, type, stem.trim(), options, correctAnswer, explanation,
                referenceAnswer, sampleAnswer, scoringRubric, difficulty, knowledgePoints, group,
                integer(node, "groupSortOrder"), QuestionFingerprint.sha256(groupMaterial, stem));
    }

    private ImportPreviewResponse.Group group(Row row, JsonNode node, List<String> errors) {
        JsonNode group = node.get("group");
        if (group == null && hasText(text(node, "groupType"))) group = node;
        if (group == null || group.isNull()) return null;
        if (!group.isObject()) {
            invalid(row, errors, "group must be an object");
            return null;
        }
        String groupType = upper(text(group, "groupType"));
        String title = text(group, "title");
        String content = text(group, "content");
        String sharedOptions = json(group.get("sharedOptions"), row, "sharedOptions", errors);
        if (!"DIALOGUE".equals(groupType) && !"READING".equals(groupType)) {
            invalid(row, errors, "groupType must be DIALOGUE or READING");
            return null;
        }
        if (!within(title, 1, 255) || !within(content, 1, 50_000)) {
            invalid(row, errors, "group requires title and content within length limits");
            return null;
        }
        if (group.get("sharedOptions") != null && sharedOptions == null) return null;
        return new ImportPreviewResponse.Group(groupType, title.trim(), text(group, "instruction"), content.trim(),
                sharedOptions, QuestionFingerprint.sha256(content, title));
    }

    private void validateGroups(List<ImportPreviewResponse.Item> items, List<String> errors) {
        for (ImportPreviewResponse.Item item : items) {
            ImportPreviewResponse.Group group = item.getGroup();
            if (group != null && "DIALOGUE".equals(group.getGroupType()) && !"DIALOGUE_BLANK".equals(item.getQuestionType())) {
                invalid(item.getRowNumber(), errors, "DIALOGUE group may contain only DIALOGUE_BLANK questions");
            }
            if (group != null && "READING".equals(group.getGroupType()) && !"READING".equals(item.getQuestionType())) {
                invalid(item.getRowNumber(), errors, "READING group may contain only READING questions");
            }
        }
    }

    private Set<Integer> invalidRows(List<String> errors) {
        Set<Integer> rows = new HashSet<>();
        for (String error : errors) {
            if (error.startsWith("row ")) {
                int end = error.indexOf(':');
                try { rows.add(Integer.parseInt(error.substring(4, end))); } catch (RuntimeException ignored) { }
            }
        }
        return rows;
    }

    private ImportPreviewResponse.Item invalid(Row row, List<String> errors, String message) {
        invalid(row.number, errors, message);
        return null;
    }

    private void invalid(int row, List<String> errors, String message) {
        errors.add("row " + row + ": " + message);
    }

    private String format(String filename, List<String> errors) {
        if (!hasText(filename)) {
            errors.add("row 0: filename is required");
            return "";
        }
        String clean = filename.trim();
        int slash = Math.max(clean.lastIndexOf('/'), clean.lastIndexOf('\\'));
        int dot = clean.lastIndexOf('.');
        if (dot <= slash + 1 || dot == clean.length() - 1) {
            errors.add("row 0: filename must end in .json or .csv");
            return "";
        }
        String extension = clean.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!"json".equals(extension) && !"csv".equals(extension)) {
            errors.add("row 0: only .json and .csv files are accepted");
        }
        return extension;
    }

    private String json(JsonNode node, Row row, String field, List<String> errors) {
        if (node == null || node.isNull() || (node.isTextual() && !hasText(node.asText()))) return null;
        try {
            if (node.isTextual()) return objectMapper.writeValueAsString(objectMapper.readTree(node.asText()));
            return objectMapper.writeValueAsString(node);
        } catch (IOException error) {
            invalid(row, errors, field + " must be valid JSON");
            return null;
        }
    }

    private String jsonArray(String value) {
        try { return objectMapper.writeValueAsString(Collections.singletonList(value)); }
        catch (IOException error) { throw new IllegalStateException(error); }
    }

    private static JsonNode groupOptions(JsonNode node) {
        JsonNode group = node.get("group");
        return group == null ? node.get("sharedOptions") : group.get("sharedOptions");
    }

    private static boolean optionsAreABCD(JsonNode node) {
        if (node == null || !node.isObject() || node.size() != 4) return false;
        for (String option : Arrays.asList("A", "B", "C", "D")) {
            if (!node.has(option) || !hasText(node.get(option).asText())) return false;
        }
        return true;
    }

    private static String answer(JsonNode node) {
        if (node == null) return null;
        if (node.isTextual()) return upper(node.asText());
        if (node.isArray() && node.size() == 1 && node.get(0).isTextual()) return upper(node.get(0).asText());
        return null;
    }

    private static boolean hasJsonContent(JsonNode node) {
        return node != null && !node.isNull() && (!node.isTextual() || hasText(node.asText()));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) return null;
        if (value.isInt()) return value.intValue();
        try { return Integer.valueOf(value.asText()); } catch (NumberFormatException error) { return null; }
    }

    private static boolean within(String value, int min, int max) {
        return value != null && value.trim().length() >= min && value.trim().length() <= max;
    }

    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private static String upper(String value) { return value == null ? null : value.trim().toUpperCase(Locale.ROOT); }

    static final class ParsedPreview {
        final int totalRows;
        final List<ImportPreviewResponse.Item> items;
        final List<String> errors;

        ParsedPreview(int totalRows, List<ImportPreviewResponse.Item> items, List<String> errors) {
            this.totalRows = totalRows;
            this.items = items;
            this.errors = errors;
        }
    }

    private static final class Row {
        final int number;
        final JsonNode node;
        Row(int number, JsonNode node) { this.number = number; this.node = node; }
    }
}

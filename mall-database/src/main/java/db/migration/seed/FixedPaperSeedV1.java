package db.migration.seed;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.CRC32;

/**
 * Frozen migration contract for fixed-degree-english-v1, not an application importer.
 * Keep this class and published JSON immutable; corrections require a new version.
 * No parsed answer-bearing node escapes this loader or becomes an API DTO.
 */
public final class FixedPaperSeedV1 {
    private static final String RESOURCE = "db/seed/fixed-degree-english-v1/";
    private static final String[] SECTIONS = {"dialogues", "readings", "vocabulary", "grammar", "translation", "writing"};
    private static final String[] TYPES = {"DIALOGUE_BLANK", "READING", "VOCABULARY", "GRAMMAR", "TRANSLATION", "WRITING"};
    private static final int[] COUNTS = {10, 20, 10, 10, 1, 1};
    private static final int[] SCORES = {1, 2, 1, 1, 15, 15};
    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private FixedPaperSeedV1() { }

    /** Includes the shipped resource bytes in Flyway history, unlike the Java default. */
    public static Integer checksum(int first, int last) {
        CRC32 crc = new CRC32();
        crc.update("FixedPaperSeedV1:1".getBytes(StandardCharsets.UTF_8));
        try {
            for (int number = first; number <= last; number++) crc.update(resource(number));
            return (int) crc.getValue();
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot checksum fixed-paper resources", failure);
        }
    }

    public static void publishBatch(Connection connection, int first, int last) throws Exception {
        require(first >= 1 && last <= 50 && last - first == 9 && (first - 1) % 10 == 0,
                "Expected one consecutive ten-paper batch");
        List<JsonNode> papers = new ArrayList<>();
        Set<String> fingerprints = new HashSet<>();
        for (int number = first; number <= last; number++) {
            JsonNode paper = JSON.readTree(resource(number));
            validate(paper, number, fingerprints);
            papers.add(paper);
        }
        // MySQL Flyway does not guarantee transactional DML for a Java migration.
        // The migration opts out of Flyway transactions so this one owns the boundary.
        require(connection.getAutoCommit(), "Fixed-paper migration requires an idle autocommit connection");
        connection.setAutoCommit(false);
        boolean transactionFinished = false;
        try {
            long blueprint = blueprint(connection);
            rejectExistingFingerprints(connection, fingerprints);
            List<Long> publications = new ArrayList<>();
            for (JsonNode paper : papers) publications.add(insertPaper(connection, blueprint, paper));
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE exam_fixed_paper SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP(3) WHERE id=? AND status='DRAFT'")) {
                for (Long id : publications) {
                    statement.setLong(1, id);
                    require(statement.executeUpdate() == 1, "Publication did not update one draft");
                }
            }
            connection.commit();
            transactionFinished = true;
        } catch (Exception | Error failure) {
            try {
                connection.rollback();
                transactionFinished = true;
            } catch (SQLException rollback) {
                failure.addSuppressed(rollback);
            }
            throw failure;
        } finally {
            // Switching to autocommit after a failed rollback could commit partial content.
            if (transactionFinished) connection.setAutoCommit(true);
        }
    }

    private static byte[] resource(int number) throws IOException {
        String name = RESOURCE + String.format(Locale.ROOT, "fixed-%02d.json", number);
        try (InputStream stream = FixedPaperSeedV1.class.getClassLoader().getResourceAsStream(name)) {
            if (stream == null) throw new IOException("Missing original simulation: " + name);
            return stream.readAllBytes();
        }
    }

    private static void validate(JsonNode paper, int number, Set<String> batchFingerprints) throws Exception {
        require(paper != null && paper.isObject(), "Expected paper object");
        require("fixed-degree-english-v1".equals(text(paper, "datasetVersion")), "Unsupported dataset");
        require(String.format(Locale.ROOT, "fixed-%02d", number).equals(text(paper, "paperCode")), "Paper code/file mismatch");
        integer(paper, "version", 1);
        integer(paper, "displayOrder", number);
        integer(paper, "durationMinutes", 120);
        integer(paper, "totalScore", 100);
        require("DEGREE_ENGLISH_2016_V2".equals(text(paper, "blueprintCode"))
                && "1.0".equals(text(paper, "blueprintVersion")), "Unexpected blueprint");
        require("ORIGINAL".equals(text(paper, "copyrightStatus")), "Original material only");
        require(text(paper, "sourceDeclaration").contains("原创模拟")
                && text(paper, "title").contains("原创模拟"), "Simulation declaration missing");
        List<String> manifest = new ArrayList<>();
        int score = 0;
        for (int section = 0; section < SECTIONS.length; section++) {
            JsonNode entries = paper.required(SECTIONS[section]);
            require(entries.isArray(), "Section must be an ordered array");
            int order = 0;
            if (section < 2) {
                require(entries.size() == (section == 0 ? 3 : 4), "Invalid group count");
                for (int index = 0; index < entries.size(); index++) {
                    JsonNode group = entries.get(index);
                    integer(group, "sectionOrder", section + 1);
                    integer(group, "groupOrder", index + 1);
                    require((section == 0 ? "DIALOGUE" : "READING").equals(text(group, "groupType")), "Invalid group type");
                    text(group, "title"); text(group, "instruction"); difficulty(group);
                    addFingerprint(group, "contentFingerprint", "GROUP", text(group, "content"), manifest, batchFingerprints);
                    JsonNode questions = group.required("questions");
                    int expected = section == 0 ? (index == 0 ? 4 : 3) : 5;
                    require(questions.isArray() && questions.size() == expected, "Invalid group item count");
                    if (section == 0) options(group.required("sharedOptions"));
                    Set<String> dialogueAnswers = new HashSet<>();
                    for (int member = 0; member < questions.size(); member++) {
                        JsonNode question = questions.get(member);
                        integer(question, "groupSortOrder", member + 1);
                        validateQuestion(question, section, ++order, manifest, batchFingerprints);
                        if (section == 0) {
                            require(group.required("content").asText().contains("(" + (member + 1) + ") _____"),
                                    "Dialogue blank missing");
                            require(group.required("sharedOptions").equals(question.required("options")), "Shared options mismatch");
                            require(dialogueAnswers.add(question.required("correctAnswer").get(0).asText()), "Reused dialogue answer");
                        }
                    }
                }
            } else {
                for (JsonNode question : entries) validateQuestion(question, section, ++order, manifest, batchFingerprints);
            }
            require(order == COUNTS[section], "Invalid section question count");
            score += order * SCORES[section];
        }
        require(score == 100, "Invalid total score");
        require(JSON.valueToTree(manifest).equals(paper.required("fingerprintManifest")), "Fingerprint manifest mismatch");
        ObjectNode unsigned = ((ObjectNode) paper).deepCopy();
        unsigned.remove("contentFingerprint");
        require(hash(canonical(unsigned)).equals(text(paper, "contentFingerprint")), "Paper content fingerprint mismatch");
    }

    private static void validateQuestion(JsonNode question, int section, int order,
                                         List<String> manifest, Set<String> fingerprints) throws Exception {
        integer(question, "sectionOrder", section + 1);
        integer(question, "itemOrder", order);
        integer(question, "score", SCORES[section]);
        require(TYPES[section].equals(text(question, "questionType")), "Wrong question type/order");
        difficulty(question);
        addFingerprint(question, "contentFingerprint", "QUESTION", text(question, "stem"), manifest, fingerprints);
        text(question, "explanation");
        if (section < 4) {
            options(question.required("options"));
            JsonNode answer = question.required("correctAnswer");
            require(answer.isArray() && answer.size() == 1 && answer.get(0).isTextual()
                    && answer.get(0).asText().matches("[A-D]"), "Invalid objective answer");
            require(!question.hasNonNull("referenceAnswer") && !question.hasNonNull("sampleAnswer")
                    && !question.hasNonNull("scoringRubric"), "Unexpected objective reference fields");
        } else {
            require(!question.hasNonNull("correctAnswer") && !question.hasNonNull("options"), "Unexpected subjective options");
            String field = section == 4 ? "referenceAnswer" : "sampleAnswer";
            addFingerprint(question, field + "Fingerprint", "QUESTION", text(question, field), manifest, fingerprints);
            JsonNode rubric = question.required("scoringRubric");
            require(rubric.isObject(), "Missing scoring rubric");
            integer(rubric, "maxScore", 15);
            JsonNode criteria = rubric.required("criteria");
            require(criteria.isArray() && criteria.size() > 0, "Missing scoring criteria");
            int maximum = 0;
            for (JsonNode criterion : criteria) {
                text(criterion, "name"); text(criterion, "description");
                require(criterion.path("maxScore").isIntegralNumber() && criterion.path("maxScore").asInt() > 0,
                        "Invalid rubric criterion");
                maximum += criterion.path("maxScore").asInt();
            }
            require(maximum == 15, "Rubric total must be fifteen");
        }
    }

    private static void difficulty(JsonNode node) {
        require(node.path("difficulty").isIntegralNumber() && node.path("difficulty").asInt() >= 2
                && node.path("difficulty").asInt() <= 4, "Difficulty must be 2–4");
        JsonNode points = node.required("knowledgePoints");
        require(points.isArray() && points.size() > 0, "Knowledge points missing");
        for (JsonNode point : points) require(point.isTextual() && !point.asText().isBlank(), "Invalid knowledge point");
    }

    private static void options(JsonNode options) {
        require(options.isObject() && options.size() == 4, "Expected four choices");
        Set<String> values = new HashSet<>();
        for (String key : new String[]{"A", "B", "C", "D"}) require(values.add(normalize(text(options, key))), "Duplicate choice");
    }

    private static void addFingerprint(JsonNode node, String field, String kind, String value,
                                       List<String> manifest, Set<String> fingerprints) throws Exception {
        String fingerprint = hash(kind + "\n" + normalize(value));
        require(fingerprint.equals(text(node, field)), "Invalid " + field);
        require(fingerprints.add(fingerprint), "Repeated original content: " + fingerprint);
        manifest.add(fingerprint);
    }

    private static long blueprint(Connection connection) throws SQLException {
        long id;
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id,duration_minutes,total_score FROM exam_blueprint WHERE blueprint_code='DEGREE_ENGLISH_2016_V2' AND version='1.0' AND status='ENABLED'");
             ResultSet rows = statement.executeQuery()) {
            require(rows.next() && rows.getInt(2) == 120 && rows.getBigDecimal(3).intValueExact() == 100,
                    "Required enabled blueprint is missing");
            id = rows.getLong(1);
            require(!rows.next(), "Ambiguous blueprint");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT section_order,question_type,group_count,answer_item_count,score_per_item,subjective FROM exam_blueprint_section WHERE blueprint_id=? ORDER BY section_order")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                for (int section = 0; section < TYPES.length; section++) {
                    require(rows.next() && rows.getInt(1) == section + 1 && TYPES[section].equals(rows.getString(2))
                            && rows.getInt(3) == (section == 0 ? 3 : section == 1 ? 4 : 0)
                            && rows.getInt(4) == COUNTS[section] && rows.getBigDecimal(5).intValueExact() == SCORES[section]
                            && rows.getBoolean(6) == (section >= 4), "Blueprint section mismatch");
                }
                require(!rows.next(), "Unexpected blueprint section");
            }
        }
        return id;
    }

    private static void rejectExistingFingerprints(Connection connection, Set<String> fingerprints) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT fingerprint FROM exam_import_fingerprint_reservation WHERE fingerprint=?")) {
            for (String fingerprint : fingerprints) {
                statement.setString(1, fingerprint);
                try (ResultSet rows = statement.executeQuery()) {
                    require(!rows.next(), "Original content already reserved: " + fingerprint);
                }
            }
        }
    }

    private static long insertPaper(Connection c, long blueprint, JsonNode paper) throws Exception {
        long source = insert(c, "INSERT INTO exam_question_source (source_name,source_type,enabled,copyright_status,review_status,copyright_note,review_comment,reviewed_at) VALUES (?,'ORIGINAL_SIMULATION',1,'ORIGINAL','APPROVED',?,'Versioned original practice publication',CURRENT_TIMESTAMP(3))",
                text(paper, "paperCode") + " original simulation v1", text(paper, "sourceDeclaration"));
        long batch = insert(c, "INSERT INTO exam_import_batch (file_name,file_format,source_id,status,total_rows,success_rows) VALUES (?,'JSON',?,'IMPORTED',52,52)",
                text(paper, "paperCode") + ".json", source);
        long fixed = insert(c, "INSERT INTO exam_fixed_paper (paper_code,version,title,blueprint_id,blueprint_version_snapshot,duration_minutes,total_score,source_id,source_declaration,content_fingerprint,display_order,status) VALUES (?,1,?,?,'1.0',120,100,?,?,?,?,'DRAFT')",
                text(paper, "paperCode"), text(paper, "title"), blueprint, source, text(paper, "sourceDeclaration"),
                text(paper, "contentFingerprint"), paper.required("displayOrder").asInt());
        int row = 0;
        for (int section = 0; section < SECTIONS.length; section++) {
            for (JsonNode entry : paper.required(SECTIONS[section])) {
                if (section < 2) {
                    String fingerprint = text(entry, "contentFingerprint");
                    reserve(c, fingerprint, "GROUP", batch);
                    // Also reserve the established V13/importer fingerprint for this exact material/title.
                    reserve(c, hash(normalize(text(entry, "content")) + "\n" + normalize(text(entry, "title"))), "GROUP", batch);
                    long originalGroup = insert(c, "INSERT INTO exam_question_group (group_type,title,instruction,content,shared_options,source_id,difficulty,knowledge_points,content_fingerprint,review_status,enabled) VALUES (?,?,?,?,?,?,?,?,?,'APPROVED',1)",
                            text(entry, "groupType"), text(entry, "title"), text(entry, "instruction"), text(entry, "content"),
                            json(entry, "sharedOptions"), source, entry.required("difficulty").asInt(), json(entry, "knowledgePoints"), fingerprint);
                    long fixedGroup = insert(c, "INSERT INTO exam_fixed_paper_group (fixed_paper_id,original_group_id,source_id,content_fingerprint,section_order,group_order,group_type,title,instruction,content,shared_options) VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                            fixed, originalGroup, source, fingerprint, section + 1, entry.required("groupOrder").asInt(),
                            text(entry, "groupType"), text(entry, "title"), text(entry, "instruction"), text(entry, "content"), json(entry, "sharedOptions"));
                    for (JsonNode question : entry.required("questions")) {
                        insertQuestion(c, question, fixed, fixedGroup, originalGroup, source, batch, ++row, text(entry, "content"));
                    }
                } else {
                    insertQuestion(c, entry, fixed, null, null, source, batch, ++row, "");
                }
            }
        }
        return fixed;
    }

    private static void insertQuestion(Connection c, JsonNode q, long fixed, Long fixedGroup, Long originalGroup,
                                       long source, long batch, int row, String material) throws Exception {
        String fingerprint = text(q, "contentFingerprint");
        reserve(c, fingerprint, "QUESTION", batch);
        reserve(c, hash(normalize(material) + "\n" + normalize(text(q, "stem"))), "QUESTION", batch);
        for (String field : new String[]{"referenceAnswerFingerprint", "sampleAnswerFingerprint"}) {
            if (q.hasNonNull(field)) reserve(c, text(q, field), "QUESTION", batch);
        }
        long original = insert(c, "INSERT INTO exam_question (group_id,question_type,stem,options,correct_answer,explanation,reference_answer,sample_answer,scoring_rubric,source_id,group_sort_order,difficulty,knowledge_points,content_fingerprint,review_status,enabled) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'APPROVED',1)",
                originalGroup, text(q, "questionType"), text(q, "stem"), json(q, "options"), json(q, "correctAnswer"),
                text(q, "explanation"), optionalText(q, "referenceAnswer"), optionalText(q, "sampleAnswer"), json(q, "scoringRubric"),
                source, originalGroup == null ? null : q.required("groupSortOrder").asInt(), q.required("difficulty").asInt(),
                json(q, "knowledgePoints"), fingerprint);
        insert(c, "INSERT INTO exam_fixed_paper_item (fixed_paper_id,fixed_paper_group_id,original_question_id,source_id,content_fingerprint,section_order,item_order,question_type,stem,options,correct_answer,explanation,reference_answer,sample_answer,scoring_rubric,knowledge_points,score) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                fixed, fixedGroup, original, source, fingerprint, q.required("sectionOrder").asInt(), q.required("itemOrder").asInt(),
                text(q, "questionType"), text(q, "stem"), json(q, "options"), json(q, "correctAnswer"), text(q, "explanation"),
                optionalText(q, "referenceAnswer"), optionalText(q, "sampleAnswer"), json(q, "scoringRubric"), json(q, "knowledgePoints"), q.required("score").asInt());
        insert(c, "INSERT INTO exam_import_item (batch_id,`row_number`,payload,question_fingerprint,group_fingerprint,status) VALUES (?,?,?,?,?,'IMPORTED')",
                batch, row, q.toString(), fingerprint, originalGroup == null ? null : hash("GROUP\n" + normalize(material)));
    }

    private static void reserve(Connection c, String fingerprint, String kind, long batch) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(
                "INSERT INTO exam_import_fingerprint_reservation (fingerprint,fingerprint_kind,batch_id) VALUES (?,?,?)")) {
            statement.setString(1, fingerprint); statement.setString(2, kind); statement.setLong(3, batch);
            statement.executeUpdate();
        }
    }

    private static long insert(Connection c, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int index = 0; index < values.length; index++) statement.setObject(index + 1, values[index]);
            require(statement.executeUpdate() == 1, "Expected one inserted seed row");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                require(keys.next(), "Missing seed row ID");
                return keys.getLong(1);
            }
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        require(value != null && value.isTextual() && !value.asText().isBlank(), "Missing text field: " + field);
        return value.asText();
    }

    private static String optionalText(JsonNode node, String field) {
        return node.hasNonNull(field) ? text(node, field) : null;
    }

    private static String json(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.get(field).toString() : null;
    }

    private static void integer(JsonNode node, String field, int expected) {
        require(node.path(field).isIntegralNumber() && node.path(field).asInt() == expected, "Invalid " + field);
    }

    private static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String hash(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(64);
        for (byte part : digest) result.append(String.format(Locale.ROOT, "%02x", part & 0xff));
        return result.toString();
    }

    /** Sorted object keys, array order retained, UTF-8 JSON; format is part of v1. */
    private static String canonical(JsonNode node) {
        if (node.isArray()) {
            List<String> values = new ArrayList<>();
            for (JsonNode value : node) values.add(canonical(value));
            return "[" + String.join(",", values) + "]";
        }
        if (node.isObject()) {
            Set<String> names = new TreeSet<>();
            Iterator<String> fields = node.fieldNames();
            fields.forEachRemaining(names::add);
            List<String> values = new ArrayList<>();
            for (String name : names) values.add(JSON.getNodeFactory().textNode(name).toString() + ":" + canonical(node.get(name)));
            return "{" + String.join(",", values) + "}";
        }
        return node.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}

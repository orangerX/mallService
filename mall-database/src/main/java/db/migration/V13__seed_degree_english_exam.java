package db.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.Normalizer;
import java.util.Locale;

/** Original simulation content. Flyway owns execution and the transaction. */
public class V13__seed_degree_english_exam extends BaseJavaMigration {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void migrate(Context context) throws Exception {
        Connection connection = context.getConnection();
        long sourceId = insertSource(connection);
        insertBlueprint(connection);
        try (PreparedStatement groups = connection.prepareStatement(
                "INSERT INTO exam_question_group (group_type,title,instruction,content,shared_options,"
                        + "source_id,difficulty,knowledge_points,content_fingerprint,review_status,enabled) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,'APPROVED',1)", Statement.RETURN_GENERATED_KEYS);
             PreparedStatement questions = connection.prepareStatement(
                "INSERT INTO exam_question (group_id,question_type,stem,options,correct_answer,explanation,"
                        + "reference_answer,sample_answer,scoring_rubric,source_id,group_sort_order,difficulty,"
                        + "knowledge_points,content_fingerprint,review_status,enabled) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,'APPROVED',1)");
             PreparedStatement reservations = connection.prepareStatement(
                "INSERT INTO exam_import_fingerprint_reservation (fingerprint,fingerprint_kind,batch_id) "
                        + "VALUES (?,?,NULL)")) {
            for (int number = 1; number <= 5; number++) {
                JsonNode set = load(String.format(Locale.ROOT, "set-%02d.json", number));
                for (String section : new String[]{"dialogues", "readings"}) {
                    for (JsonNode group : set.required(section)) {
                        String material = group.required("content").asText();
                        String fingerprint = fingerprint(material, group.required("title").asText());
                        groups.setString(1, group.required("groupType").asText());
                        groups.setString(2, group.required("title").asText());
                        text(groups, 3, group, "instruction");
                        groups.setString(4, material);
                        json(groups, 5, group, "sharedOptions");
                        groups.setLong(6, sourceId);
                        groups.setInt(7, group.required("difficulty").asInt());
                        json(groups, 8, group, "knowledgePoints");
                        groups.setString(9, fingerprint);
                        groups.executeUpdate();
                        long groupId = generatedKey(groups);
                        reserve(reservations, fingerprint, "GROUP");
                        int order = 0;
                        for (JsonNode question : group.required("questions")) {
                            addQuestion(questions, reservations, question, sourceId, groupId, ++order, material);
                        }
                    }
                }
                for (String section : new String[]{"vocabulary", "grammar", "translation", "writing"}) {
                    for (JsonNode question : set.required(section)) {
                        addQuestion(questions, reservations, question, sourceId, null, null, "");
                    }
                }
                // Bound each batch to one complete set, in file order.
                reservations.executeBatch();
                reservations.clearBatch();
                questions.executeBatch();
                questions.clearBatch();
            }
        }
    }

    private JsonNode load(String name) throws IOException {
        String resource = "db/seed/degree-english-v1/" + name;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) throw new IOException("Missing original exam seed: " + resource);
            return mapper.readTree(input);
        }
    }

    private long insertSource(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO exam_question_source (source_name,source_type,enabled,copyright_status,"
                        + "review_status,copyright_note,review_comment,reviewed_at) "
                        + "VALUES (?,'ORIGINAL_SIMULATION',1,'ORIGINAL','APPROVED',?,?,CURRENT_TIMESTAMP(3))",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "ORIGINAL_SIMULATION_2026");
            statement.setString(2, "degree-english-v1：五套原创模拟材料，不是官方真题；未复制第三方题库。首套改编自本项目原创样卷。");
            statement.setString(3, "随版本发布的原创模拟题种子；用于学习练习。");
            statement.executeUpdate();
            return generatedKey(statement);
        }
    }

    private void insertBlueprint(Connection connection) throws SQLException {
        long blueprintId;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO exam_blueprint (blueprint_code,blueprint_name,version,duration_minutes,total_score,status) "
                        + "VALUES ('DEGREE_ENGLISH_2016_V2',?,'1.0',120,100,'ENABLED')",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "成人学士学位英语原创模拟考试（2016年第二版结构）");
            statement.executeUpdate();
            blueprintId = generatedKey(statement);
        }
        String[] types = {"DIALOGUE_BLANK", "READING", "VOCABULARY", "GRAMMAR", "TRANSLATION", "WRITING"};
        int[] groupCounts = {3, 4, 0, 0, 0, 0};
        int[] itemCounts = {10, 20, 10, 10, 1, 1};
        int[] scores = {1, 2, 1, 1, 15, 15};
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO exam_blueprint_section (blueprint_id,section_order,question_type,group_count,"
                        + "answer_item_count,score_per_item,subjective) VALUES (?,?,?,?,?,?,?)")) {
            for (int index = 0; index < types.length; index++) {
                statement.setLong(1, blueprintId);
                statement.setInt(2, index + 1);
                statement.setString(3, types[index]);
                statement.setInt(4, groupCounts[index]);
                statement.setInt(5, itemCounts[index]);
                statement.setInt(6, scores[index]);
                statement.setBoolean(7, index >= 4);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void addQuestion(PreparedStatement questions, PreparedStatement reservations, JsonNode question,
                             long sourceId, Long groupId, Integer order, String material) throws Exception {
        String stem = question.required("stem").asText();
        String fingerprint = fingerprint(material, stem);
        if (groupId == null) questions.setNull(1, Types.BIGINT); else questions.setLong(1, groupId);
        questions.setString(2, question.required("questionType").asText());
        questions.setString(3, stem);
        json(questions, 4, question, "options");
        json(questions, 5, question, "correctAnswer");
        text(questions, 6, question, "explanation");
        text(questions, 7, question, "referenceAnswer");
        text(questions, 8, question, "sampleAnswer");
        json(questions, 9, question, "scoringRubric");
        questions.setLong(10, sourceId);
        if (order == null) questions.setNull(11, Types.INTEGER); else questions.setInt(11, order);
        questions.setInt(12, question.required("difficulty").asInt());
        json(questions, 13, question, "knowledgePoints");
        questions.setString(14, fingerprint);
        questions.addBatch();
        reserve(reservations, fingerprint, "QUESTION");
    }

    private static void reserve(PreparedStatement statement, String fingerprint, String kind) throws SQLException {
        statement.setString(1, fingerprint);
        statement.setString(2, kind);
        statement.addBatch();
    }

    private static long generatedKey(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (!keys.next()) throw new SQLException("Exam seed insert returned no generated key");
            return keys.getLong(1);
        }
    }

    private static void text(PreparedStatement statement, int index, JsonNode node, String field) throws SQLException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) statement.setNull(index, Types.LONGVARCHAR);
        else statement.setString(index, value.asText());
    }

    private static void json(PreparedStatement statement, int index, JsonNode node, String field) throws SQLException {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) statement.setNull(index, Types.LONGVARCHAR);
        else statement.setString(index, value.toString());
    }

    // Frozen Task 3 contract: NFC, lowercase, collapsed whitespace, newline separator.
    // Local to keep the historical migration independent of future domain code changes.
    private static String fingerprint(String material, String stem) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                (normalize(material) + "\n" + normalize(stem)).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder(64);
        for (byte value : digest) hex.append(String.format(Locale.ROOT, "%02x", value & 0xff));
        return hex.toString();
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFC).toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }
}


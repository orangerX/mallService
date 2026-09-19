package com.mall.exam.importer;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Static SQL contracts complement service tests without requiring a shared MySQL database. */
class ExamImportMapperContractTest {
    @Test
    void unusedReservationCleanupProtectsActualRowsAndActiveBatchOwners() throws Exception {
        String cleanup=normalized(statement("releaseUnusedFingerprint"));
        assertTrue(cleanup.contains("WHERE fingerprint=?"));
        assertTrue(cleanup.contains("NOT EXISTS (SELECT 1 FROM exam_question WHERE content_fingerprint=?)"));
        assertTrue(cleanup.contains("NOT EXISTS (SELECT 1 FROM exam_question_group WHERE content_fingerprint=?)"));
        assertTrue(cleanup.contains("batch_id IS NULL OR NOT EXISTS"));
        assertTrue(cleanup.contains("b.id=exam_import_fingerprint_reservation.batch_id"));
        assertTrue(cleanup.contains("b.status IN ('UPLOADED','VALIDATED')"));
        for(String read:Arrays.asList("lockFingerprint","fingerprintQuestionIds","fingerprintGroupIds"))
            assertTrue(normalized(statement(read)).endsWith("FOR UPDATE"));
    }

    @Test
    void reservationReleaseIsGuardedByBothFingerprintAndBatchOwnership() throws Exception {
        BoundSql release = statement("releaseFingerprint");
        assertEquals("DELETE FROM exam_import_fingerprint_reservation WHERE fingerprint=? AND batch_id=?", normalized(release));
        assertEquals(Arrays.asList("fingerprint", "batchId"), release.getParameterMappings().stream()
                .map(ParameterMapping::getProperty).collect(Collectors.toList()));
    }

    @Test
    void terminalTransitionAtomicallyWritesAdminAndAllCountsWithValidatedGuard() throws Exception {
        BoundSql update = statement("markImported");
        assertEquals("UPDATE exam_import_batch SET status='IMPORTED', imported_by=COALESCE(imported_by,?), success_rows=?, failed_rows=?, duplicate_rows=?, duplicate_warnings=? WHERE id=? AND status='VALIDATED' AND (imported_by IS NULL OR imported_by=?)", normalized(update));
        assertEquals(Arrays.asList("adminId", "successRows", "failedRows", "duplicateRows", "duplicateWarnings", "batchId", "adminId"),
                update.getParameterMappings().stream().map(ParameterMapping::getProperty).collect(Collectors.toList()));
        assertTrue(normalized(statement("findBatchByIdForUpdate")).endsWith("FOR UPDATE"));
        assertTrue(normalized(statement("findItemsByBatchIdForUpdate")).endsWith("FOR UPDATE"));
    }

    @Test
    void v12KeepsUniqueFlattenedRowsAndReservationBackfill() throws Exception {
        Path module = Path.of(System.getProperty("basedir", ".")).toAbsolutePath();
        String schema = Files.readString(module.resolve("../mall-database/src/main/resources/db/migration/V12__create_exam_domain.sql"));
        assertTrue(schema.contains("UNIQUE KEY uk_exam_import_item_batch_row (batch_id, row_number)"));
        assertTrue(schema.contains("duplicate_rows INT UNSIGNED NOT NULL DEFAULT 0"));
        assertTrue(schema.contains("PRIMARY KEY (fingerprint)"));
        assertTrue(schema.contains("SELECT content_fingerprint, 'GROUP', NULL FROM exam_question_group"));
        assertTrue(schema.contains("SELECT content_fingerprint, 'QUESTION', NULL FROM exam_question"));
    }

    private BoundSql statement(String id) throws Exception {
        String resource = "mapper/exam/ImportBatchMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        return configuration.getMappedStatement("com.mall.exam.importer.mapper.ImportBatchMapper." + id).getBoundSql(null);
    }

    private String normalized(BoundSql sql) {
        return sql.getSql().trim().replaceAll("\\s+", " ");
    }
}

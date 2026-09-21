-- Nullable additions preserve existing random papers and their immutable snapshots.
-- Code/version are frozen on the attempt so resuming it never depends on publication status.
ALTER TABLE exam_paper
    ADD COLUMN fixed_paper_id BIGINT UNSIGNED NULL,
    ADD COLUMN fixed_paper_code VARCHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    ADD COLUMN fixed_paper_version INT UNSIGNED NULL,
    ADD KEY idx_exam_paper_fixed_template (fixed_paper_id),
    ADD KEY idx_exam_paper_user_fixed_code (user_id, fixed_paper_code),
    ADD CONSTRAINT fk_exam_paper_fixed_template FOREIGN KEY (fixed_paper_id) REFERENCES exam_fixed_paper (id),
    ADD CONSTRAINT ck_exam_paper_fixed_identity CHECK (
        (fixed_paper_id IS NULL AND fixed_paper_code IS NULL AND fixed_paper_version IS NULL)
        OR (fixed_paper_id IS NOT NULL AND fixed_paper_code IS NOT NULL
            AND fixed_paper_version IS NOT NULL AND fixed_paper_version > 0)
    );

package db.migration;

import db.migration.seed.FixedPaperSeedV1;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Three original simulations with exam-length reading passages. */
public class V34__seed_fixed_papers_103_to_105 extends BaseJavaMigration {
    @Override
    public Integer getChecksum() { return FixedPaperSeedV1.checksum(103, 105); }

    @Override
    public boolean canExecuteInTransaction() { return false; }

    @Override
    public void migrate(Context context) throws Exception {
        FixedPaperSeedV1.publishBatch(context.getConnection(), 103, 105);
    }
}

package db.migration;

import db.migration.seed.FixedPaperSeedV1;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Eighteen original simulations with exam-length reading passages. */
public class V30__seed_fixed_papers_63_to_80 extends BaseJavaMigration {
    @Override
    public Integer getChecksum() { return FixedPaperSeedV1.checksum(63, 80); }

    @Override
    public boolean canExecuteInTransaction() { return false; }

    @Override
    public void migrate(Context context) throws Exception {
        FixedPaperSeedV1.publishBatch(context.getConnection(), 63, 80);
    }
}

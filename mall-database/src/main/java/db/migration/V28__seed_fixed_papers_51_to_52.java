package db.migration;

import db.migration.seed.FixedPaperSeedV1;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Two original simulations with exam-length reading passages. */
public class V28__seed_fixed_papers_51_to_52 extends BaseJavaMigration {
    @Override
    public Integer getChecksum() { return FixedPaperSeedV1.checksum(51, 52); }

    @Override
    public boolean canExecuteInTransaction() { return false; }

    @Override
    public void migrate(Context context) throws Exception {
        FixedPaperSeedV1.publishBatch(context.getConnection(), 51, 52);
    }
}

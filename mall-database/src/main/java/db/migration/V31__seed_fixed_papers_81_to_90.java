package db.migration;

import db.migration.seed.FixedPaperSeedV1;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Ten original simulations with exam-length reading passages. */
public class V31__seed_fixed_papers_81_to_90 extends BaseJavaMigration {
    @Override
    public Integer getChecksum() { return FixedPaperSeedV1.checksum(81, 90); }

    @Override
    public boolean canExecuteInTransaction() { return false; }

    @Override
    public void migrate(Context context) throws Exception {
        FixedPaperSeedV1.publishBatch(context.getConnection(), 81, 90);
    }
}

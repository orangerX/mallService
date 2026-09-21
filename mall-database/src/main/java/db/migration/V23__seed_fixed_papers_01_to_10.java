package db.migration;

import db.migration.seed.FixedPaperSeedV1;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Original simulations 01–10. The frozen loader owns this MySQL DML transaction. */
public class V23__seed_fixed_papers_01_to_10 extends BaseJavaMigration {
    @Override
    public Integer getChecksum() { return FixedPaperSeedV1.checksum(1, 10); }

    @Override
    public boolean canExecuteInTransaction() { return false; }

    @Override
    public void migrate(Context context) throws Exception {
        FixedPaperSeedV1.publishBatch(context.getConnection(), 1, 10);
    }
}

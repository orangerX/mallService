package com.mall.exam.importer.mapper;

import com.mall.exam.importer.model.ImportBatchEntity;
import com.mall.exam.importer.model.ImportBatchItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ImportBatchMapper {
    int insertBatch(ImportBatchEntity batch);
    int insertItem(ImportBatchItemEntity item);
    ImportBatchEntity findBatchByIdForUpdate(@Param("batchId") long batchId);
    List<ImportBatchItemEntity> findItemsByBatchIdForUpdate(@Param("batchId") long batchId);
    int reserveFingerprint(@Param("fingerprint") String fingerprint, @Param("kind") String kind, @Param("batchId") Long batchId);
    String lockFingerprint(@Param("fingerprint") String fingerprint);
    List<Long> fingerprintQuestionIds(@Param("fingerprint") String fingerprint);
    List<Long> fingerprintGroupIds(@Param("fingerprint") String fingerprint);
    int releaseUnusedFingerprint(@Param("fingerprint") String fingerprint);
    int releaseFingerprint(@Param("fingerprint") String fingerprint, @Param("batchId") long batchId);
    int markItemImported(@Param("itemId") long itemId);
    int markItemDuplicate(@Param("itemId") long itemId);
    int markImported(@Param("batchId") long batchId, @Param("adminId") long adminId,
                     @Param("successRows") int successRows, @Param("failedRows") int failedRows,
                     @Param("duplicateRows") int duplicateRows, @Param("duplicateWarnings") String duplicateWarnings);
}

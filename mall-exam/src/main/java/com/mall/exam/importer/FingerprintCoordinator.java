package com.mall.exam.importer;

import com.mall.common.api.ErrorCode;
import com.mall.common.exception.BusinessException;
import com.mall.exam.importer.mapper.ImportBatchMapper;
import org.springframework.http.HttpStatus;
import java.util.*;

/**
 * Shared transactional boundary for imported and manually edited content.
 * Callers hold their transaction through content writes and reservation cleanup.
 * Manual reservations have no batch owner and are rolled back with failed edits.
 */
public final class FingerprintCoordinator {
    private final ImportBatchMapper mapper;
    public FingerprintCoordinator(ImportBatchMapper mapper) { this.mapper=mapper; }

    public Map<String,Boolean> acquire(Map<String,String> desired,Set<String> retained,Long batchId) {
        SortedSet<String> keys=new TreeSet<>(retained);keys.addAll(desired.keySet());
        Map<String,Boolean> acquired=new HashMap<>();
        for(String key:keys) {
            if(retained.contains(key)) {
                mapper.lockFingerprint(key);
                if(desired.containsKey(key)) acquired.put(key,true);
                continue;
            }
            // SQL removes only unused reservations whose owner is absent or terminal.
            mapper.releaseUnusedFingerprint(key);
            boolean inserted=mapper.reserveFingerprint(key,desired.get(key),batchId)==1;
            mapper.lockFingerprint(key);
            // Current locking reads also protect legacy rows without a reservation.
            boolean referenced=!mapper.fingerprintQuestionIds(key).isEmpty()
                    || !mapper.fingerprintGroupIds(key).isEmpty();
            acquired.put(key,inserted && !referenced);
        }
        return acquired;
    }

    public void requireAll(Map<String,String> desired,Set<String> retained) {
        Map<String,Boolean> result=acquire(desired,retained,null);
        if(result.values().stream().anyMatch(value->!value))
            throw new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT);
    }

    public void releaseOwned(String fingerprint,long batchId) {
        if(mapper.releaseFingerprint(fingerprint,batchId)!=1)
            throw new BusinessException(HttpStatus.CONFLICT,ErrorCode.DATA_CONFLICT);
    }

    public void releaseObsolete(Set<String> previous,Set<String> current) {
        for(String key:new TreeSet<>(previous)) if(!current.contains(key)) mapper.releaseUnusedFingerprint(key);
    }
}

package com.mall.exam.importer;

import com.mall.exam.importer.mapper.ImportBatchMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class FingerprintCoordinatorTest {
    ImportBatchMapper mapper=mock(ImportBatchMapper.class);
    FingerprintCoordinator coordinator=new FingerprintCoordinator(mapper);
    @Test void acquisitionLocksOldAndNewKeysInOneLexicalOrderAndChecksRealRows() {
        when(mapper.reserveFingerprint(anyString(),anyString(),isNull())).thenReturn(1);
        when(mapper.fingerprintQuestionIds("b")).thenReturn(Collections.singletonList(8L));
        Map<String,String> desired=new HashMap<>();desired.put("b","QUESTION");desired.put("c","QUESTION");
        Map<String,Boolean> result=coordinator.acquire(desired,Collections.singleton("a"),null);
        assertFalse(result.get("b"));assertTrue(result.get("c"));
        InOrder order=inOrder(mapper);
        order.verify(mapper).lockFingerprint("a");
        order.verify(mapper).lockFingerprint("b");
        order.verify(mapper).lockFingerprint("c");
    }
    @Test void onlyOwnedAcquisitionsAreReleasedWhenImportGroupCannotBeUsed() {
        when(mapper.releaseFingerprint("a",9L)).thenReturn(1);
        coordinator.releaseOwned("a",9L);
        verify(mapper).releaseFingerprint("a",9L);
        verifyNoMoreInteractions(mapper);
    }
    @Test void obsoleteReleaseDelegatesToReferenceAndInactiveBatchGuard() {
        coordinator.releaseObsolete(new HashSet<>(Arrays.asList("a","b")),Collections.singleton("b"));
        verify(mapper).releaseUnusedFingerprint("a");
        verifyNoMoreInteractions(mapper);
    }
}

package com.mall.exam.blueprint;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DialogueCapacityCalculatorTest {
    @Test void countsAlternativeTwoFourFourPartition() {
        assertEquals(2, DialogueCapacityCalculator.maximumPapers(frequencies(2,2,4,4,4,4)));
    }
    @Test void maximizesDisjointTriplesInsteadOfTakingFirstGreedyMatch() {
        // Taking 1+4+5 leaves no second triple; 1+4+5 is not optimal here:
        // 2+3+5 and 2+4+4 use six groups, leaving the one-blank group.
        assertEquals(2, DialogueCapacityCalculator.maximumPapers(frequencies(1,2,2,3,4,4,5)));
    }
    @Test void rejectsAggregateCapacityWithoutAnExactTriple() {
        assertEquals(0, DialogueCapacityCalculator.maximumPapers(frequencies(2,2,2,2,2)));
        assertEquals(0, DialogueCapacityCalculator.maximumPapers(frequencies(10,10,10)));
    }
    @Test void countsRepeatedSizesAndDoesNotMutateInputs() {
        Map<Integer,Long> counts=frequencies(1,1,8,3,3,4);
        assertEquals(2, DialogueCapacityCalculator.maximumPapers(counts));
        assertEquals(2L,counts.get(1));
    }
    private Map<Integer,Long> frequencies(int... sizes) {
        Map<Integer,Long> counts=new HashMap<>();
        for(int size:sizes) counts.merge(size,1L,Long::sum);
        return counts;
    }
}

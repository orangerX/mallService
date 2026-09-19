package com.mall.exam.blueprint;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class DialogueCapacityCalculatorTest {
    @Test void agreesWithIndependentBruteForceForEverySmallCountVector() {
        for(int mask=0;mask<6561;mask++) {
            int value=mask;int[] counts=new int[9];Map<Integer,Long> input=new HashMap<>();
            for(int size=1;size<=8;size++){counts[size]=value%3;value/=3;input.put(size,(long)counts[size]);}
            assertEquals(brute(counts,new HashMap<>()),DialogueCapacityCalculator.maximumPapers(input),"vector "+mask);
        }
    }
    @Test void realisticThousandGroupBankHasBoundedRuntime() {
        Map<Integer,Long> input=new HashMap<>();
        for(int size=1;size<=8;size++)input.put(size,125L);
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),()->{
            long result=DialogueCapacityCalculator.maximumPapers(input);
            assertTrue(result>0 && result<=333);
        });
    }
    @Test void constructedThousandGroupBankReachesTheExactCountBound() {
        // 100*(1+1+8), 100*(2+4+4), 133*(3+3+4), plus one unusable group.
        Map<Integer,Long> input=new HashMap<>();
        input.put(1,200L);input.put(2,100L);input.put(3,266L);
        input.put(4,333L);input.put(8,100L);input.put(10,1L);
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(5),()->
                assertEquals(333,DialogueCapacityCalculator.maximumPapers(input)));
    }
    private long brute(int[] counts,Map<String,Long> memo) {
        String key=Arrays.toString(counts);if(memo.containsKey(key))return memo.get(key);
        long result=0;
        for(int a=1;a<=8;a++)for(int b=a;b<=8;b++){
            int c=10-a-b;if(c<b||c>8)continue;
            int[] next=counts.clone();next[a]--;next[b]--;next[c]--;
            if(next[a]>=0&&next[b]>=0&&next[c]>=0)result=Math.max(result,1+brute(next,memo));
        }
        memo.put(key,result);return result;
    }
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

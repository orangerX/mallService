package com.mall.exam.blueprint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Maximum disjoint selections of three complete groups with exactly ten answer items. */
public final class DialogueCapacityCalculator {
    private static final List<int[]> PATTERNS = patterns();

    private DialogueCapacityCalculator() {}

    public static long maximumPapers(Map<Integer, Long> frequencies) {
        long[] counts = new long[9];
        for (int size = 1; size <= 8; size++) {
            counts[size] = Math.max(0, frequencies.getOrDefault(size, 0L));
        }
        Search search = new Search();
        search.visit(counts, 0, 0);
        return search.best;
    }

    private static List<int[]> patterns() {
        List<int[]> result = new ArrayList<>();
        for (int first = 1; first <= 8; first++) {
            for (int second = first; second <= 8; second++) {
                int third = 10 - first - second;
                if (third < second || third > 8) continue;
                int[] pattern = new int[9];
                pattern[first]++;
                pattern[second]++;
                pattern[third]++;
                result.add(pattern);
            }
        }
        return result;
    }

    private static long available(long[] counts, int[] pattern) {
        long copies = Long.MAX_VALUE;
        for (int size = 1; size <= 8; size++) {
            if (pattern[size] > 0) copies = Math.min(copies, counts[size] / pattern[size]);
        }
        return copies;
    }

    private static final class Search {
        private long best;

        private void visit(long[] counts, int index, long papers) {
            best = Math.max(best, papers);
            if (index == PATTERNS.size()) return;
            // Only sizes occurring in a still-feasible pattern can contribute to the bound.
            boolean[] useful = new boolean[9];
            for (int i = index; i < PATTERNS.size(); i++) {
                int[] pattern = PATTERNS.get(i);
                if (available(counts, pattern) == 0) continue;
                for (int size = 1; size <= 8; size++) useful[size] |= pattern[size] > 0;
            }
            long groups = 0, items = 0;
            for (int size = 1; size <= 8; size++) {
                if (useful[size]) { groups += counts[size]; items += counts[size] * size; }
            }
            if (papers + Math.min(groups / 3, items / 10) <= best) return;
            int[] pattern = PATTERNS.get(index);
            long maximum = available(counts, pattern);
            for (long copies = maximum; copies >= 0; copies--) {
                long[] remaining = counts.clone();
                for (int size = 1; size <= 8; size++) remaining[size] -= copies * pattern[size];
                visit(remaining, index + 1, papers + copies);
                if (best == papers + Math.min(groups / 3, items / 10)) return;
            }
        }
    }
}

package com.mall.exam.blueprint;

import java.math.BigInteger;
import java.util.*;

/** Exact integer packing of the eight possible positive triples summing to ten. */
public final class DialogueCapacityCalculator {
    private static final List<int[]> PATTERNS = patterns();
    private DialogueCapacityCalculator() {}

    public static long maximumPapers(Map<Integer, Long> frequencies) {
        long[] counts = new long[9], upper = new long[PATTERNS.size()];
        for (int size = 1; size <= 8; size++) counts[size] = Math.max(0, frequencies.getOrDefault(size, 0L));
        for (int i = 0; i < upper.length; i++) {
            upper[i] = Long.MAX_VALUE;
            for (int size = 1; size <= 8; size++) if (PATTERNS.get(i)[size] > 0)
                upper[i] = Math.min(upper[i], counts[size] / PATTERNS.get(i)[size]);
        }
        Search search = new Search(counts);
        search.visit(new long[upper.length], upper);
        return search.best;
    }

    private static List<int[]> patterns() {
        List<int[]> result = new ArrayList<>();
        for (int a = 1; a <= 8; a++) for (int b = a; b <= 8; b++) {
            int c = 10 - a - b;
            if (c < b || c > 8) continue;
            int[] pattern = new int[9]; pattern[a]++; pattern[b]++; pattern[c]++;
            result.add(pattern);
        }
        return result;
    }

    private static final class Search {
        private final long[] counts;
        private long best;
        private Search(long[] counts) { this.counts = counts; }
        private void visit(long[] lower, long[] upper) {
            Relaxation solution = relax(counts, lower, upper);
            if (solution == null || solution.value.floor() <= best) return;
            long rounded = 0;
            int fractional = -1;
            for (int i = 0; i < lower.length; i++) {
                rounded += solution.variables[i].floor();
                if (!solution.variables[i].integer()) fractional = i;
            }
            // All coefficients are nonnegative: rounding down remains feasible.
            best = Math.max(best, rounded);
            if (fractional < 0 || solution.value.floor() <= best) return;
            // These children partition every remaining integer solution. Exact
            // rational LP bounds avoid any floating-point tolerance pruning.
            long floor = solution.variables[fractional].floor();
            long[] higher = lower.clone(); higher[fractional] = floor + 1;
            visit(higher, upper);
            long[] smaller = upper.clone(); smaller[fractional] = floor;
            visit(lower, smaller);
        }
    }

    private static Relaxation relax(long[] counts, long[] lower, long[] upper) {
        int n = PATTERNS.size(), m = 8 + n, rhs = n + m;
        Fraction[][] t = new Fraction[m + 1][rhs + 1];
        int[] basis = new int[m];
        for (int row = 0; row <= m; row++) Arrays.fill(t[row], Fraction.ZERO);
        long base = 0;
        for (int col = 0; col < n; col++) {
            if (lower[col] > upper[col]) return null;
            base += lower[col]; t[m][col] = Fraction.of(-1);
        }
        for (int row = 0; row < m; row++) {
            long remaining;
            if (row < 8) {
                int size = row + 1;
                remaining = counts[size];
                for (int col = 0; col < n; col++) {
                    int coefficient = PATTERNS.get(col)[size];
                    remaining -= coefficient * lower[col];
                    t[row][col] = Fraction.of(coefficient);
                }
            } else {
                int col = row - 8;
                remaining = upper[col] - lower[col]; t[row][col] = Fraction.ONE;
            }
            if (remaining < 0) return null;
            t[row][rhs] = Fraction.of(remaining);
            t[row][n + row] = Fraction.ONE; basis[row] = n + row;
        }
        // The slack basis is feasible after shifting by the integer lower bounds.
        // Bland's rule prevents cycling in degenerate zero-capacity rows.
        while (true) {
            int entering = -1;
            for (int col = 0; col < rhs; col++) if (t[m][col].sign() < 0) { entering = col; break; }
            if (entering < 0) break;
            int leaving = -1; Fraction ratio = null;
            for (int row = 0; row < m; row++) if (t[row][entering].sign() > 0) {
                Fraction candidate = t[row][rhs].divide(t[row][entering]);
                if (ratio == null || candidate.compareTo(ratio) < 0
                        || (candidate.compareTo(ratio) == 0 && basis[row] < basis[leaving])) {
                    leaving = row; ratio = candidate;
                }
            }
            if (leaving < 0) throw new IllegalStateException("Bounded dialogue LP became unbounded");
            Fraction pivot = t[leaving][entering];
            for (int col = 0; col <= rhs; col++) t[leaving][col] = t[leaving][col].divide(pivot);
            for (int row = 0; row <= m; row++) if (row != leaving && t[row][entering].sign() != 0) {
                Fraction factor = t[row][entering];
                for (int col = 0; col <= rhs; col++)
                    t[row][col] = t[row][col].subtract(factor.multiply(t[leaving][col]));
            }
            basis[leaving] = entering;
        }
        Fraction[] variables = new Fraction[n];
        for (int col = 0; col < n; col++) variables[col] = Fraction.of(lower[col]);
        for (int row = 0; row < m; row++) if (basis[row] < n)
            variables[basis[row]] = variables[basis[row]].add(t[row][rhs]);
        return new Relaxation(t[m][rhs].add(Fraction.of(base)), variables);
    }

    private static final class Relaxation {
        final Fraction value;
        final Fraction[] variables;
        Relaxation(Fraction value, Fraction[] variables) { this.value = value; this.variables = variables; }
    }

    /** Exact arithmetic for the fixed-size optimizer, independent of bank size. */
    private static final class Fraction implements Comparable<Fraction> {
        static final Fraction ZERO = new Fraction(BigInteger.ZERO, BigInteger.ONE);
        static final Fraction ONE = new Fraction(BigInteger.ONE, BigInteger.ONE);
        final BigInteger numerator, denominator;
        Fraction(BigInteger numerator, BigInteger denominator) {
            if (denominator.signum() < 0) { numerator = numerator.negate(); denominator = denominator.negate(); }
            BigInteger gcd = numerator.gcd(denominator);
            this.numerator = numerator.divide(gcd); this.denominator = denominator.divide(gcd);
        }
        static Fraction of(long value) { return value == 0 ? ZERO : value == 1 ? ONE : new Fraction(BigInteger.valueOf(value), BigInteger.ONE); }
        int sign() { return numerator.signum(); }
        boolean integer() { return denominator.equals(BigInteger.ONE); }
        long floor() { return numerator.divide(denominator).longValueExact(); } // Used only for nonnegative solutions/objectives.
        Fraction add(Fraction other) {
            if (sign() == 0) return other; if (other.sign() == 0) return this;
            return new Fraction(numerator.multiply(other.denominator).add(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
        }
        Fraction subtract(Fraction other) {
            if (other.sign() == 0) return this;
            return new Fraction(numerator.multiply(other.denominator).subtract(other.numerator.multiply(denominator)), denominator.multiply(other.denominator));
        }
        Fraction multiply(Fraction other) {
            if (sign() == 0 || other.sign() == 0) return ZERO;
            return new Fraction(numerator.multiply(other.numerator), denominator.multiply(other.denominator));
        }
        Fraction divide(Fraction other) {
            if (sign() == 0) return ZERO;
            return new Fraction(numerator.multiply(other.denominator), denominator.multiply(other.numerator));
        }
        public int compareTo(Fraction other) { return numerator.multiply(other.denominator).compareTo(other.numerator.multiply(denominator)); }
    }
}

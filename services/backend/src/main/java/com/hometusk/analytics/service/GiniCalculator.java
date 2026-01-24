package com.hometusk.analytics.service;

import java.util.Arrays;

public final class GiniCalculator {

    private GiniCalculator() {}

    public static Double calculate(int[] workloads) {
        if (workloads == null || workloads.length == 0) {
            return null;
        }

        long sum = Arrays.stream(workloads).asLongStream().sum();
        if (sum == 0) {
            return null; // No tasks -> N/A
        }

        int n = workloads.length;
        if (n == 1) {
            return 0.0; // Single member -> perfect equality
        }

        int[] sorted = Arrays.copyOf(workloads, n);
        Arrays.sort(sorted);

        double numerator = 0;
        for (int i = 0; i < n; i++) {
            numerator += (2.0 * (i + 1) - n - 1) * sorted[i];
        }

        double gini = Math.abs(numerator / (n * sum));
        return Math.min(gini, 1.0); // Clamp to 0..1
    }

    public static Integer toBalance(Double gini) {
        if (gini == null) {
            return null;
        }
        return (int) Math.round((1 - gini) * 100);
    }
}

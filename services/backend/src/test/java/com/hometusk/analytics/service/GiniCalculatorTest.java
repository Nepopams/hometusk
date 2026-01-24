package com.hometusk.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import org.junit.jupiter.api.Test;

class GiniCalculatorTest {

    @Test
    void calculate_equalDistribution_returnsZero() {
        Double gini = GiniCalculator.calculate(new int[] {5, 5, 5});
        assertThat(gini).isCloseTo(0.0, offset(1e-6));
    }

    @Test
    void calculate_completeInequality_returnsHigh() {
        Double gini = GiniCalculator.calculate(new int[] {10, 0, 0});
        assertThat(gini).isCloseTo(0.6666667, offset(1e-6));
    }

    @Test
    void calculate_typicalDistribution_returnsMedium() {
        Double gini = GiniCalculator.calculate(new int[] {2, 6, 8});
        assertThat(gini).isCloseTo(0.25, offset(1e-6));
    }

    @Test
    void calculate_emptyArray_returnsNull() {
        assertThat(GiniCalculator.calculate(new int[] {})).isNull();
    }

    @Test
    void calculate_nullArray_returnsNull() {
        assertThat(GiniCalculator.calculate(null)).isNull();
    }

    @Test
    void calculate_allZeros_returnsNull() {
        assertThat(GiniCalculator.calculate(new int[] {0, 0, 0})).isNull();
    }

    @Test
    void calculate_singleNonZero_returnsZero() {
        Double gini = GiniCalculator.calculate(new int[] {7});
        assertThat(gini).isCloseTo(0.0, offset(1e-6));
    }

    @Test
    void toBalance_nullGini_returnsNull() {
        assertThat(GiniCalculator.toBalance(null)).isNull();
    }

    @Test
    void toBalance_zeroGini_returns100() {
        assertThat(GiniCalculator.toBalance(0.0)).isEqualTo(100);
    }

    @Test
    void toBalance_halfGini_returns50() {
        assertThat(GiniCalculator.toBalance(0.5)).isEqualTo(50);
    }
}

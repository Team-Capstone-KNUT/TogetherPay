package com.devcrew.togetherpay.domain.openChatAI.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceDistanceCalculatorTest {

    private final PlaceDistanceCalculator calculator = new PlaceDistanceCalculator();

    @Test
    void returnsNullWhenAnyCoordinateIsMissing() {
        Integer result = calculator.calculateMeters(null, 139.7006, 35.6580, 139.7016);

        assertThat(result).isNull();
    }

    @Test
    void returnsZeroForSameCoordinate() {
        Integer result = calculator.calculateMeters(35.6896, 139.7006, 35.6896, 139.7006);

        assertThat(result).isZero();
    }

    @Test
    void calculatesApproximateDistanceBetweenShinjukuAndShibuya() {
        Integer result = calculator.calculateMeters(35.6896, 139.7006, 35.6580, 139.7016);

        assertThat(result).isBetween(3400, 3700);
    }
}

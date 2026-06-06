package com.devcrew.togetherpay.domain.openChatAI.service;

import org.springframework.stereotype.Component;

@Component
public class PlaceDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6371000;

    public Integer calculateMeters(Double originLat, Double originLng, Double targetLat, Double targetLng) {
        if (originLat == null || originLng == null || targetLat == null || targetLng == null) {
            return null;
        }

        double lat1 = Math.toRadians(originLat);
        double lat2 = Math.toRadians(targetLat);
        double deltaLat = Math.toRadians(targetLat - originLat);
        double deltaLng = Math.toRadians(targetLng - originLng);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return (int) Math.round(EARTH_RADIUS_METERS * c);
    }
}

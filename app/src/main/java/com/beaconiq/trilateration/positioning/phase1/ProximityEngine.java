/**
 * Extraido de: BeaconsIQ_Project/TEDtour/app/src/main/java/
 *   com/ited/org/ec/tedtour/model/ProximityEngine.java
 * Lineas: 1-53
 * Fecha de extraccion: 2026-04-28
 * Proposito: Phase I — modelo original de TEDtour, fiel 1:1
 */
package com.beaconiq.trilateration.positioning.phase1;

import java.util.*;
import java.util.stream.Collectors;

public class ProximityEngine {

    private final Map<String, BeaconSample> beaconMap;
    private String currentInsideBeacon = null;
    private double influenceRadius;

    public ProximityEngine(Map<String, BeaconSample> beaconMap, double influenceRadius) {
        this.beaconMap = beaconMap;
        this.influenceRadius = influenceRadius;
    }

    public void setInfluenceRadius(double meters) {
        this.influenceRadius = meters;
    }

    public ProximityResult evaluate() {
        // Get top 3 beacons with smallest filtered distance
        List<BeaconSample> top3 = beaconMap.values().stream()
                .filter(b -> b.getFilteredDistance() != null)
                .sorted(Comparator.comparingDouble(BeaconSample::getFilteredDistance))
                .limit(3)
                .collect(Collectors.toList());

        if (top3.size() < 3) return ProximityResult.searching();

        // Find closest beacon within influence zone
        BeaconSample closest = null;
        double minDist = Double.MAX_VALUE;
        for (BeaconSample b : top3) {
            double d = b.getFilteredDistance();
            if (d < minDist) {
                minDist = d;
                closest = b;
            }
        }

        if (minDist <= influenceRadius) {
            if (!closest.getUid().equals(currentInsideBeacon)) {
                currentInsideBeacon = closest.getUid();
                return ProximityResult.entered(closest.getUid(), minDist);
            }
            return ProximityResult.inside();
        }

        currentInsideBeacon = null;
        return ProximityResult.positioning();
    }
}

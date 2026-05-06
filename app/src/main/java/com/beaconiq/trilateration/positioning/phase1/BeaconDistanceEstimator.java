/**
 * Extraido de: BeaconsIQ_Project/TEDtour/app/src/main/java/
 *   com/ited/org/ec/tedtour/util/BeaconDistanceEstimator.java
 * Lineas: 1-49
 * Fecha de extraccion: 2026-04-28
 * Proposito: Phase I — modelo original de TEDtour, fiel 1:1
 */
package com.beaconiq.trilateration.positioning.phase1;

import android.util.Log;

public class BeaconDistanceEstimator {
    private static final String TAG = "BeaconDistanceEstimator";

    // RSSI values for the two beacons
    private int beacon1_rssi;
    private int beacon2_rssi;

    // Constructor to initialize the RSSI values
    public BeaconDistanceEstimator(int beacon1_rssi, int beacon2_rssi) {
        this.beacon1_rssi = beacon1_rssi;
        this.beacon2_rssi = beacon2_rssi;
    }

    // Method to estimate distance using triangulation
    public double estimateDistance() {
        // Empirical constants for distance estimation
        double txPower = -59; // This value depends on the beacon manufacturer and model
        double n = 2.0; // Path loss exponent

        // Convert RSSI to distance for each beacon
        double distance1 = calculateDistance(beacon1_rssi, txPower, n);
        double distance2 = calculateDistance(beacon2_rssi, txPower, n);

        // Perform triangulation to estimate the distance from the smartphone to the midpoint between the beacons
        double distance = Math.sqrt(distance1 * distance1 + distance2 * distance2);

        Log.d(TAG, "Estimated distance from smartphone to midpoint between beacons: " + distance + " meters");

        return distance;
    }

    // Method to calculate distance from RSSI using the log-distance path loss model
    private double calculateDistance(int rssi, double txPower, double n) {
        if (rssi == 0) {
            return -1.0; // Invalid RSSI value
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
            return (n == 0) ? -1.0 : (Math.pow(ratio, n));
        }
    }
}

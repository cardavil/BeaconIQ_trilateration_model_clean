/**
 * Extraido de: BeaconsIQ_Project/TEDtour/app/src/main/java/
 *   com/ited/org/ec/tedtour/model/ProximityResult.java
 * Lineas: 1-21
 * Fecha de extraccion: 2026-04-28
 * Proposito: Phase I — modelo original de TEDtour, fiel 1:1
 */
package com.beaconiq.trilateration.positioning.phase1;

public class ProximityResult {

    public enum State { SEARCHING, POSITIONING, INSIDE_ZONE, ENTERED_ZONE }

    public final State state;
    public final String beaconUid;
    public final Double distance;

    private ProximityResult(State state, String uid, Double distance) {
        this.state = state;
        this.beaconUid = uid;
        this.distance = distance;
    }

    public static ProximityResult searching() { return new ProximityResult(State.SEARCHING, null, null); }
    public static ProximityResult positioning() { return new ProximityResult(State.POSITIONING, null, null); }
    public static ProximityResult inside() { return new ProximityResult(State.INSIDE_ZONE, null, null); }
    public static ProximityResult entered(String uid, double d) { return new ProximityResult(State.ENTERED_ZONE, uid, d); }
}

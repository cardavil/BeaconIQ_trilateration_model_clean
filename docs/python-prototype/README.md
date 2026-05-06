# Python reference prototype

Original Python implementation of the BLE trilateration model.

**Status:** Reference only. Being ported to Java in `app/src/main/java/com/beaconiq/trilateration/`.

## Files

- `trikalman.py` — Live BLE scanner + RSSI→distance + 1D Kalman per beacon + 
  nonlinear least squares trilateration + 2D Kalman on position. Uses `bleak` 
  for scanning and `scipy.optimize.least_squares` for the solve.
- `iBKS_Trilateration_Model.py` — Minimal static prototype. Hardcoded distances, 
  single least-squares solve. Predates `trikalman.py`.

## Known parameters (not calibrated, will be re-fitted in Java)

- `PATH_LOSS_N = 2.4`
- `DEFAULT_TXP = -58`
- Kalman 1D (distance): q=0.05, r=0.25
- Kalman 2D (position): q=0.05, r=0.15

## Beacon test layout used in prototype

- B1: (0.0, 0.0)
- B2: (5.84, 0.0)
- B3: (5.10, 5.03)

These are the physical positions in the test room. They will be configurable 
(not hardcoded) in the Java port.

## Do not run this code from this directory.

This is documentation. The active implementation is the Android app.

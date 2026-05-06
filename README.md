# BeaconIQ

Android app for BLE beacon testing, indoor signal calibration, and real-time positioning. Part of the TEDtour indoor positioning project.

## What it does

- Scans for BLE beacons (iBeacon, AltBeacon, Eddystone) via AltBeacon library
- Two positioning modes selectable from toolbar menu: Phase I (TEDtour original) and Phase II (Kalman + WCL). Toolbar shows BeaconIQ in teal bold + current screen name in muted text
- Real-time 2D positioning canvas with beacon map and estimated position
- Records timed sessions of RSSI signal data
- Uploads session data to Google Sheets via Apps Script backend
- 10 configurable model parameters in Phase II (TxPower, Path Loss N, RSSI threshold, Kalman q/r, RSSI buffer/window, scale factor, beacon timeout, eval interval) with "MODEL PARAMETERS" header

## Quick start

```bash
git clone https://github.com/cardavil/BeaconIQ_trilateration_model.git
cd BeaconIQ_trilateration_model
./gradlew :app:assembleDebug
```

Install on an Android device with BLE 4.0+ and grant Bluetooth/Location permissions.

## Phase I vs Phase II

| | Phase I (original) | Phase II (improved) |
|---|---|---|
| Kalman filter | OFF (bypassed) | ON (q/r configurable, default 0.05/0.25) |
| Parameters | Hardcoded (TxPower=-59, N=2.0, threshold=-100) | 10 params configurable from UI |
| Solver | findClosestBeacon | Centroid or WCL |
| Position output | Nearest beacon's (x,y) | Weighted average of all beacons |
| ibeaconHits metric | Counts after RSSI threshold filter | Counts after RSSI threshold filter |
| GAS session cols | 26 (params visible, defaults sent) | 26 (all params sent with configured values) |

## Project structure

| Directory | Description |
|---|---|
| `app/` | Android application (Explore, Signal Recording Phase I/II, Permissions) |
| `app/.../positioning/phase1/` | TEDtour-identical positioning (8 files, hardcoded params, all bugs preserved) |
| `app/.../positioning/phase2/` | BeaconIQ modified positioning (3 files: BeaconSample, KalmanFilter1D, TrilaterationJavaSolver) |
| `backend/apps-script/` | Google Apps Script backend + web console |
| `docs/python-prototype/` | Original Python prototype |
| `docs/code-archive/` | Archived manual iBeacon parser |
| `docs/code-changes-log.md` | Detailed change log |

See [STATUS.md](STATUS.md) for detailed architecture and current state.

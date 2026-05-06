package com.beaconiq.trilateration.scan;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class BleScanner {

    public interface ScanListener {
        void onBeaconDiscovered(Beacon beacon, byte[] scanRecord);
        void onGenericDeviceDiscovered(BleDevice device);
        void onScanFailed(int errorCode);
    }

    private static final String TAG = "BeaconIQ";

    private final Context appContext;
    private ScanListener listener;
    private boolean isScanning;

    private final BeaconManager beaconManager;
    private final Region allRegion = new Region("all-beacons", null, null, null);

    private BluetoothLeScanner leScanner;
    private final ScanCallback genericCallback;
    private final Set<String> knownBeaconMacs = new CopyOnWriteArraySet<>();
    private final Map<String, byte[]> rawBytesCache = new ConcurrentHashMap<>();

    public BleScanner(Context context) {
        appContext = context.getApplicationContext();

        beaconManager = BeaconManager.getInstanceForApplication(appContext);
        beaconManager.getBeaconParsers().clear();

        // iBeacon (Apple)
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"));
        // AltBeacon
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        // Eddystone-UID
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"));
        // Eddystone-URL
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("s:0-1=feaa,m:2-2=10,p:3-3:-41,i:4-20v"));
        // Eddystone-TLM
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("x:0-1=feaa,m:2-2=20,d:3-3,d:4-5,d:6-7,d:8-11,d:12-15"));
        // URIBeacon (legacy)
        beaconManager.getBeaconParsers().add(new BeaconParser()
                .setBeaconLayout("s:0-1=fed8,m:2-2=00,p:3-3:-41,i:4-21v"));

        beaconManager.addRangeNotifier((beacons, region) -> {
            if (listener == null) return;
            for (Beacon b : beacons) {
                knownBeaconMacs.add(b.getBluetoothAddress());
                Log.d(TAG, "Beacon: " + b.getId1()
                        + " txPower=" + b.getTxPower()
                        + " rssi=" + b.getRssi());
                byte[] cached = rawBytesCache.remove(b.getBluetoothAddress());
                listener.onBeaconDiscovered(b, cached);
            }
        });

        genericCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                routeGeneric(result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                for (ScanResult r : results) routeGeneric(r);
            }

            @Override
            public void onScanFailed(int errorCode) {
                if (listener != null) listener.onScanFailed(errorCode);
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void routeGeneric(ScanResult result) {
        if (listener == null) return;
        String mac = result.getDevice().getAddress();
        byte[] bytes = result.getScanRecord() != null
                ? result.getScanRecord().getBytes() : null;
        if (knownBeaconMacs.contains(mac)) {
            if (bytes != null) rawBytesCache.put(mac, bytes);
            return;
        }
        long now = System.currentTimeMillis();
        String name = result.getDevice().getName();
        listener.onGenericDeviceDiscovered(
                new BleDevice(mac, name, result.getRssi(), now, bytes));
    }

    public void setListener(ScanListener listener) {
        this.listener = listener;
    }

    public boolean isScanning() {
        return isScanning;
    }

    @SuppressLint("MissingPermission")
    public void startScan() {
        if (isScanning) return;

        try {
            beaconManager.startRangingBeacons(allRegion);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start beacon ranging", e);
            if (listener != null) listener.onScanFailed(-1);
            return;
        }

        BluetoothManager manager =
                (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            BluetoothAdapter adapter = manager.getAdapter();
            if (adapter != null && adapter.isEnabled()) {
                leScanner = adapter.getBluetoothLeScanner();
                if (leScanner != null) {
                    ScanSettings settings = new ScanSettings.Builder()
                            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                            .build();
                    leScanner.startScan(new ArrayList<>(), settings, genericCallback);
                }
            }
        }

        isScanning = true;
    }

    @SuppressLint("MissingPermission")
    public void stopScan() {
        if (!isScanning) return;

        try {
            beaconManager.stopRangingBeacons(allRegion);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop beacon ranging", e);
        }

        if (leScanner != null) {
            try {
                leScanner.stopScan(genericCallback);
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop generic scanner", e);
            }
        }

        rawBytesCache.clear();
        isScanning = false;
    }
}

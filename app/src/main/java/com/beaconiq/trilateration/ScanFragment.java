package com.beaconiq.trilateration;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.beaconiq.trilateration.scan.BleDevice;
import com.beaconiq.trilateration.scan.BleScanner;
import com.beaconiq.trilateration.ui.DeviceListAdapter;

import org.altbeacon.beacon.Beacon;

public class ScanFragment extends Fragment implements BleScanner.ScanListener {

    private static final long STALE_CHECK_INTERVAL_MS = 1000;
    private static final long STALE_DEVICE_AGE_MS = 10_000;

    private TextView statusText;
    private Button scanButton;
    private BleScanner bleScanner;
    private DeviceListAdapter adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean wasScanning;

    private final Runnable staleRunnable = new Runnable() {
        @Override
        public void run() {
            if (adapter != null) {
                adapter.removeStaleDevices(STALE_DEVICE_AGE_MS);
            }
            handler.postDelayed(this, STALE_CHECK_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_scan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusText = view.findViewById(R.id.status_text);
        scanButton = view.findViewById(R.id.scan_button);
        RecyclerView deviceList = view.findViewById(R.id.device_list);

        bleScanner = new BleScanner(requireContext());
        bleScanner.setListener(this);

        adapter = new DeviceListAdapter();
        deviceList.setLayoutManager(new LinearLayoutManager(requireContext()));
        deviceList.setAdapter(adapter);

        handler.postDelayed(staleRunnable, STALE_CHECK_INTERVAL_MS);

        setStatus("Idle", R.color.text_dim);
        scanButton.setOnClickListener(v -> toggleScan());
    }

    private void toggleScan() {
        if (bleScanner.isScanning()) {
            bleScanner.stopScan();
            scanButton.setText("Start Scan");
            scanButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.teal)));
            scanButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            setStatus("Stopped", R.color.text_dim);
        } else {
            bleScanner.startScan();
            scanButton.setText("Stop Scan");
            scanButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.surface_2)));
            scanButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_alert));
            setStatus("Scanning", R.color.status_ok);
        }
    }

    private void setStatus(String label, int dotColorRes) {
        String text = "● " + label;
        SpannableString span = new SpannableString(text);
        span.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(), dotColorRes)),
                0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        span.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.text_muted)),
                2, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        statusText.setText(span);
    }

    public void onPermissionsResult(boolean allGranted) {
        if (!allGranted && statusText != null) {
            statusText.setText("Permissions denied — cannot scan");
            scanButton.setEnabled(false);
        }
    }

    public void stopScanForRecording() {
        wasScanning = bleScanner.isScanning();
        if (wasScanning) {
            bleScanner.stopScan();
            scanButton.setText("Start Scan");
            scanButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.teal)));
            scanButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
            setStatus("Paused (recording)", R.color.status_warn);
        }
        scanButton.setEnabled(false);
    }

    public void resumeAfterRecording() {
        scanButton.setEnabled(true);
        if (wasScanning) {
            bleScanner.startScan();
            scanButton.setText("Stop Scan");
            scanButton.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.surface_2)));
            scanButton.setTextColor(ContextCompat.getColor(requireContext(), R.color.status_alert));
            setStatus("Scanning", R.color.status_ok);
        } else {
            setStatus("Idle", R.color.text_dim);
        }
        wasScanning = false;
    }

    @Override
    public void onBeaconDiscovered(Beacon beacon, byte[] scanRecord) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (adapter != null) adapter.updateBeacon(beacon);
            });
        }
    }

    @Override
    public void onGenericDeviceDiscovered(BleDevice device) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (adapter != null) adapter.updateDevice(device);
            });
        }
    }

    @Override
    public void onScanFailed(int errorCode) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (statusText != null) {
                    statusText.setText("Scan failed: " + errorCode);
                    scanButton.setText("Start Scan");
                    scanButton.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.teal)));
                    scanButton.setTextColor(
                            ContextCompat.getColor(requireContext(), R.color.text_primary));
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (statusText != null && scanButton != null) {
            boolean allGranted = checkAllPermissions();
            if (allGranted && !scanButton.isEnabled()) {
                scanButton.setEnabled(true);
                setStatus("Idle", R.color.text_dim);
            } else if (!allGranted) {
                statusText.setText("Permissions denied — cannot scan");
                scanButton.setEnabled(false);
            }
        }
    }

    private boolean checkAllPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (bleScanner != null && bleScanner.isScanning()) {
            bleScanner.stopScan();
            if (scanButton != null) {
                scanButton.setText("Start Scan");
                scanButton.setBackgroundTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), R.color.teal)));
                scanButton.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_primary));
                setStatus("Stopped", R.color.text_dim);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacks(staleRunnable);
        if (bleScanner != null && bleScanner.isScanning()) {
            bleScanner.stopScan();
        }
    }
}

package com.beaconiq.trilateration.scan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class BleScannerRoutingTest {

    @Test
    public void bleDevice_carriesScanRecord() {
        byte[] record = {0x02, 0x01, 0x06, (byte) 0xFF};
        BleDevice device = new BleDevice("AA:BB:CC:DD:EE:FF", "Test",
                -67, System.currentTimeMillis(), record);

        assertThat(device.getScanRecord()).isEqualTo(record);
        assertThat(device.getMacAddress()).isEqualTo("AA:BB:CC:DD:EE:FF");
        assertThat(device.getRssi()).isEqualTo(-67);
    }

    @Test
    public void bleDevice_nullScanRecord() {
        BleDevice device = new BleDevice("AA:BB:CC:DD:EE:FF", "Test",
                -67, System.currentTimeMillis());

        assertThat(device.getScanRecord()).isNull();
    }

    @Test
    public void bleDevice_legacyConstructorCompat() {
        BleDevice a = new BleDevice("AA:BB:CC:DD:EE:FF", "Test", -67, 1000L);
        BleDevice b = new BleDevice("AA:BB:CC:DD:EE:FF", "Test", -67, 1000L, null);

        assertThat(a).isEqualTo(b);
    }
}

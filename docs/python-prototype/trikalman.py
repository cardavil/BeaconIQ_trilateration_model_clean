import asyncio
import statistics
import datetime
from bleak import BleakScanner
import numpy as np
from scipy.optimize import least_squares
import matplotlib.pyplot as plt

# Coordenadas conocidas de los beacons
BEACONS = {
    "D5:0A:A3:C8:62:86": (0.0, 0.0), # CE:18:C0:29:AF:9F
    "FB:8F:7C:1F:23:06": (5.84, 0.0),
    "E0:3D:4B:FD:32:95": (5.10, 5.03),
}

SCAN_TIME = 10
DEFAULT_TXP = -58
PATH_LOSS_N = 2.4

# --- Kalman filters ---

class KalmanFilter1D:
    """Kalman 1D para suavizar series de distancia."""
    def __init__(self, q=0.05, r=0.5, x0=None, p0=1.0):
        self.q = q  # ruido del proceso
        self.r = r  # ruido de la medición
        self.x = x0
        self.p = p0

    def update(self, z):
        if self.x is None:  # inicialización con la primera medición
            self.x = z
        # Predicción (modelo estático)
        self.p += self.q
        # Ganancia de Kalman
        k = self.p / (self.p + self.r)
        # Actualización
        self.x += k * (z - self.x)
        self.p *= (1 - k)
        return self.x

class KalmanFilter2D:
    """Kalman 2D simple (posición) para suavizar (x,y) sin velocidad explícita."""
    def __init__(self, q=0.05, r=0.5, x0=None, y0=None, p0=1.0):
        self.q = q
        self.r = r
        self.x = x0
        self.y = y0
        self.px = p0
        self.py = p0

    def update(self, zx, zy):
        # Inicialización con la primera observación
        if self.x is None or self.y is None:
            self.x, self.y = zx, zy
        # Predicción (modelo estático para suavizado)
        self.px += self.q
        self.py += self.q
        # Ganancias
        kx = self.px / (self.px + self.r)
        ky = self.py / (self.py + self.r)
        # Actualización
        self.x += kx * (zx - self.x)
        self.y += ky * (zy - self.y)
        self.px *= (1 - kx)
        self.py *= (1 - ky)
        return self.x, self.y

# Instancias globales (se mantienen entre escaneos si corres varias veces)
KF_POS = KalmanFilter2D(q=0.05, r=0.15)  # ajusta q/r según el “temblor” que veas
KF_DIST = {addr: KalmanFilter1D(q=0.05, r=0.25) for addr in BEACONS}  # opcional

# --- Funciones auxiliares ---

def rssi_to_distance(rssi_avg, txp, n=PATH_LOSS_N):
    return 10 ** ((txp - rssi_avg) / (10.0 * n))

def trilateration_nonlinear(beacons, distances, x0=(1.0, 1.0)):
    P = np.array(list(beacons.values()), dtype=float)
    r = np.array(distances, dtype=float)

    def residuals(xy):
        diffs = P - xy
        dists = np.linalg.norm(diffs, axis=1)
        return dists - r

    res = least_squares(residuals, np.array(x0, dtype=float), method="trf")
    return res.x, res.cost, res.success

# --- Escaneo BLE ---

async def run():
    rssi_buffers = {addr: [] for addr in BEACONS}
    txp_seen = {addr: None for addr in BEACONS}

    def callback(device, advertisement_data):
        addr = device.address
        if addr in BEACONS:
            rssi = advertisement_data.rssi
            if rssi is not None and -85 < rssi < -40:  # filtrar outliers
                rssi_buffers[addr].append(rssi)
            for _, data in advertisement_data.manufacturer_data.items():
                if len(data) >= 23:
                    tx_power = int.from_bytes(data[22:23], byteorder="big", signed=True)
                    txp_seen[addr] = tx_power

    scanner = BleakScanner(callback)
    await scanner.start()
    print(f"Escaneando durante {SCAN_TIME} segundos...")
    await asyncio.sleep(SCAN_TIME)
    await scanner.stop()
    print("Escaneo finalizado.\n")

    # Calcular promedio RSSI y distancia
    distances_raw = []
    distances_filtered = []
    for addr, pos in BEACONS.items():
        buf = rssi_buffers[addr]
        if buf:
            rssi_avg = statistics.mean(buf)
            txp = txp_seen[addr] if txp_seen[addr] is not None else DEFAULT_TXP
            d = rssi_to_distance(rssi_avg, txp)
            distances_raw.append(d)

            # Filtrado Kalman 1D opcional en distancia
            d_filt = KF_DIST[addr].update(d)
            distances_filtered.append(d_filt)

            print(f"{addr} -> d={d:.2f} m (RSSI_avg={rssi_avg:.1f} dBm, TxP={txp} dBm, muestras={len(buf)})")
        else:
            print(f"{addr} -> sin muestras")
            distances_raw.append(None)
            distances_filtered.append(None)

    # Usa distancias filtradas si están disponibles, sino las crudas
    distances_for_trilat = []
    for d_filt, d_raw in zip(distances_filtered, distances_raw):
        distances_for_trilat.append(d_filt if d_filt is not None else d_raw)

    # Trilateración si hay las tres distancias
    if all(d is not None for d in distances_for_trilat):
        pos, cost, ok = trilateration_nonlinear(BEACONS, distances_for_trilat, x0=(1.0, 1.0))
        print(f"\nPosición estimada (no lineal): ({pos[0]:.2f}, {pos[1]:.2f})")
        print(f"Costo del ajuste: {cost:.4f}, éxito: {ok}")

        # Kalman 2D sobre la posición
        x_kf, y_kf = KF_POS.update(pos[0], pos[1])
        print(f"Posición filtrada Kalman: ({x_kf:.2f}, {y_kf:.2f})")

        # --- Gráfico ---
        fig, ax = plt.subplots()

        # Dibujar beacons y sus círculos
        for (addr, (x, y)), d in zip(BEACONS.items(), distances_for_trilat):
            ax.scatter(x, y, c='blue', marker='o')
            ax.text(x+0.05, y+0.05, addr, fontsize=8)
            circle = plt.Circle((x, y), d, color='blue', fill=False, linestyle='--', alpha=0.7)
            ax.add_patch(circle)

        # Posición estimada y posición filtrada
        ax.scatter(pos[0], pos[1], c='red', marker='x', s=90, label='Posición estimada')
        ax.scatter(x_kf, y_kf, c='purple', marker='D', s=60, label='Posición Kalman')

        ax.set_aspect('equal', adjustable='box')
        ax.set_xlabel("X (m)")
        ax.set_ylabel("Y (m)")
        ax.legend()
        plt.title("Trilateración BLE con filtrado Kalman")
        plt.grid(True)
        plt.show()

    else:
        print("\nNo se pudo calcular la posición: faltan distancias de uno o más beacons.")

asyncio.run(run())
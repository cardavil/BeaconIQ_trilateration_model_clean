# import numpy as np
# from scipy.optimize import least_squares
#
# # Beacon positions in meters (x, y)
# beacons = {
#     "B1": (0.0, 0.0),
#     "B2": (0.0, 2.8),
#     "B3": (1.4, 2.42)
# }
#
# # Estimated distances from receiver to each beacon (in meters)
# distances = {
#     "B1": 4.5,
#     "B2": 0.87,
#     "B3": 2.55
# }
#
# # Residual function: difference between actual and estimated distances
# def residuals(pos, beacons, distances):
#     x, y = pos
#     res = []
#     for bid, (bx, by) in beacons.items():
#         d_est = np.sqrt((x - bx)**2 + (y - by)**2)
#         res.append(d_est - distances[bid])
#     return res
#
# # Initial guess: center of beacon triangle
# x0 = np.mean([b[0] for b in beacons.values()])
# y0 = np.mean([b[1] for b in beacons.values()])
#
# # Solve for receiver position
# # result = least_squares(residuals, x0=[x0, y0], args=(beacons, distances))
# result = least_squares(residuals, [x0, y0], args=(beacons, distances))
# print("Estimated position:", result.x)


import numpy as np
from scipy.optimize import least_squares

# Beacon positions in meters (x, y)
beacons = {
    "B1": (0.0, 0.0),
    "B2": (5.84, 0.0),
    "B3": (5.1, 5.03

    )
}

# Estimated distances from receiver to each beacon (in meters)
distances = {
    "B1": 6.5,
    "B2": 3.91,
    "B3": 1.19
}

def residuals(pos, beacons, distances):
    x, y = pos
    res = []
    for bid, (bx, by) in beacons.items():
        d_est = np.sqrt((x - bx)**2 + (y - by)**2)
        res.append(d_est - distances[bid])
    return res

x0 = np.mean([b[0] for b in beacons.values()])
y0 = np.mean([b[1] for b in beacons.values()])

result = least_squares(residuals, [x0, y0], args=(beacons, distances))
print("Estimated position:", result.x)
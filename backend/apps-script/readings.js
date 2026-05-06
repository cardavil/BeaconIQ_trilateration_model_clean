// ═══════════════════════════════════════════════════════════════
// readings.js — Write iBeacon parsed readings
// ═══════════════════════════════════════════════════════════════

function writeReadings(sessionId, readings) {
  var sh = _ensureSheet(_p().READINGS_SHEET_ID, TAB_READINGS, HEADERS_READINGS);

  var rows = [];
  for (var i = 0; i < readings.length; i++) {
    var r = readings[i];
    rows.push([
      sessionId,
      r.timestamp_ms       || '',
      r.beacon_id          || '',
      r.uuid               || '',
      r.major              || '',
      r.minor              || '',
      r.rssi_raw           || '',
      r.rssi_filtered      || '',
      r.distance_m         || '',
      r.est_x              || '',
      r.est_y              || '',
      r.model_phase        || '',
      r.dist_no_kalman     || '',
      r.radar_closest      || '',
      r.scan_nearest_rssi  || '',
      r.service_closest    || '',
      r.dual_conflict      || ''
    ]);
  }

  if (rows.length > 0) {
    var startRow = sh.getLastRow() + 1;
    sh.getRange(startRow, 1, rows.length, HEADERS_READINGS.length)
      .setValues(rows);
  }

  Logger.log('[writeReadings] Wrote ' + rows.length + ' rows for ' + sessionId);
}

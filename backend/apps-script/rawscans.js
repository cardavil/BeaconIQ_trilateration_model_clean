// ═══════════════════════════════════════════════════════════════
// rawscans.js — Write raw BLE scan results
// ═══════════════════════════════════════════════════════════════

function writeRawScans(sessionId, rawScans) {
  var sh = _ensureSheet(_p().RAWSCANS_SHEET_ID, TAB_RAWSCANS, HEADERS_RAWSCANS);

  var rows = [];
  for (var i = 0; i < rawScans.length; i++) {
    var s = rawScans[i];
    rows.push([
      sessionId,
      s.timestamp_ms    || '',
      s.device_address  || '',
      s.company_id      || '',
      s.rssi            || '',
      s.was_ibeacon === true ? 'TRUE' : 'FALSE',
      s.data_hex        || '',
      s.reject_reason   || ''
    ]);
  }

  if (rows.length > 0) {
    var startRow = sh.getLastRow() + 1;
    sh.getRange(startRow, 1, rows.length, HEADERS_RAWSCANS.length)
      .setValues(rows);
  }

  Logger.log('[writeRawScans] Wrote ' + rows.length + ' rows for ' + sessionId);
}

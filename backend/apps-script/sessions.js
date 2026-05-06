// ═══════════════════════════════════════════════════════════════
// sessions.js — Session recording orchestrator
// ═══════════════════════════════════════════════════════════════

function recordSession(data) {
  var session  = data.session;
  var readings = data.readings  || [];
  var rawScans = data.raw_scans || [];

  if (!session || typeof session !== 'object')
    throw new Error('Missing session object in payload');

  var lock = LockService.getScriptLock();
  lock.waitLock(10000);

  var sessionId;
  try {
    var sh = _ensureSheet(_p().SESSIONS_SHEET_ID, TAB_SESSIONS, HEADERS_SESSIONS);
    sessionId = _nextSessionId(sh);

    var row = [
      sessionId,
      session.timestamp_start     || _ts(),
      session.timestamp_end       || _ts(),
      session.analyst             || '',
      session.duration_sec        || 0,
      session.movement_mode       || '',
      session.room                || '',
      session.phone_position      || '',
      session.phone_model         || '',
      session.android_version     || '',
      session.txpower             || '',
      session.path_loss_n         || '',
      session.rssi_threshold      || '',
      session.beacons_detected    || 0,
      session.total_scan_results  || 0,
      session.ibeacon_hits        || 0,
      session.rejected_count      || 0,
      session.model_phase         || '',
      session.kalman_q            || '',
      session.kalman_r            || '',
      session.rssi_buffer_size    || '',
      session.rssi_time_window_ms || '',
      session.scale_factor        || '',
      session.beacon_timeout_ms   || '',
      session.eval_interval_ms    || '',
      session.notes               || ''
    ];

    sh.appendRow(row);
  } finally {
    lock.releaseLock();
  }

  if (readings.length > 0) {
    writeReadings(sessionId, readings);
  }

  if (rawScans.length > 0) {
    writeRawScans(sessionId, rawScans);
  }

  Logger.log('[recordSession] OK — ' + sessionId +
    ' | readings=' + readings.length +
    ' | rawScans=' + rawScans.length);

  return {
    error: false,
    session_id: sessionId,
    readings_count: readings.length,
    raw_scans_count: rawScans.length
  };
}

function listSessions() {
  var sh = _ensureSheet(_p().SESSIONS_SHEET_ID, TAB_SESSIONS, HEADERS_SESSIONS);
  var last = sh.getLastRow();
  if (last < 2) return [];

  var data = sh.getRange(2, 1, last - 1, HEADERS_SESSIONS.length).getValues();

  var result = [];
  for (var i = data.length - 1; i >= 0; i--) {
    var row = data[i];
    result.push({
      session_id: String(row[0]),
      date: row[1] ? _ts(new Date(row[1])) : '',
      room: row[6] || '',
      duration_sec: row[4] || 0,
      ibeacon_hits: row[15] || 0
    });
  }
  return result;
}

function _nextSessionId(sh) {
  var last = sh.getLastRow();
  if (last < 2) return 'BIQ-0001';
  var prev = sh.getRange(last, 1).getValue();
  var num  = Number(String(prev).replace(/\D/g, '')) || 0;
  return 'BIQ-' + String(num + 1).padStart(4, '0');
}

function _findSessionRow(sessionId) {
  var sh = _ensureSheet(_p().SESSIONS_SHEET_ID, TAB_SESSIONS, HEADERS_SESSIONS);
  var last = sh.getLastRow();
  if (last < 2) return null;
  var data = sh.getRange(2, 1, last - 1, HEADERS_SESSIONS.length).getValues();
  for (var i = 0; i < data.length; i++) {
    if (String(data[i][0]) === sessionId) return data[i];
  }
  return null;
}

function _readSessionReadings(sessionId) {
  var sh = _ensureSheet(_p().READINGS_SHEET_ID, TAB_READINGS, HEADERS_READINGS);
  var last = sh.getLastRow();
  if (last < 2) return [];
  var data = sh.getRange(2, 1, last - 1, HEADERS_READINGS.length).getValues();
  var result = [];
  for (var i = 0; i < data.length; i++) {
    if (String(data[i][0]) === sessionId) result.push(data[i]);
  }
  return result;
}

function _readSessionRawScans(sessionId) {
  var sh = _ensureSheet(_p().RAWSCANS_SHEET_ID, TAB_RAWSCANS, HEADERS_RAWSCANS);
  var last = sh.getLastRow();
  if (last < 2) return [];
  var data = sh.getRange(2, 1, last - 1, HEADERS_RAWSCANS.length).getValues();
  var result = [];
  for (var i = 0; i < data.length; i++) {
    if (String(data[i][0]) === sessionId) result.push(data[i]);
  }
  return result;
}

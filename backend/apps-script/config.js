// ═══════════════════════════════════════════════════════════════
// config.js — Cached properties, timestamps, schema definitions
// ═══════════════════════════════════════════════════════════════

var _cachedProps = null;

function _p() {
  if (!_cachedProps)
    _cachedProps = PropertiesService.getScriptProperties().getProperties();
  return _cachedProps;
}

function _ts(fecha) {
  return Utilities.formatDate(
    fecha || new Date(),
    Session.getScriptTimeZone(),
    'yyyy-MM-dd HH:mm:ss'
  );
}

// ── Tab names (must match the actual sheet tab names) ──────────

var TAB_SESSIONS  = 'BeaconIQ_Sessions';
var TAB_READINGS  = 'BeaconIQ_Readings';
var TAB_RAWSCANS  = 'BeaconIQ_RawScans';

// ── Column headers (written on first use if sheet is empty) ────

var HEADERS_SESSIONS = [
  'session_id', 'timestamp_start', 'timestamp_end', 'analyst',
  'duration_sec', 'movement_mode', 'room', 'phone_position',
  'phone_model', 'android_version', 'txpower', 'path_loss_n',
  'rssi_threshold', 'beacons_detected', 'total_scan_results',
  'ibeacon_hits', 'rejected_count', 'model_phase',
  'kalman_q', 'kalman_r',
  'rssi_buffer_size', 'rssi_time_window_ms', 'scale_factor',
  'beacon_timeout_ms', 'eval_interval_ms',
  'notes'
];

var HEADERS_READINGS = [
  'session_id', 'timestamp_ms', 'beacon_id', 'uuid',
  'major', 'minor', 'rssi_raw', 'rssi_filtered',
  'distance_m', 'est_x', 'est_y', 'model_phase',
  'dist_no_kalman', 'radar_closest', 'scan_nearest_rssi',
  'service_closest', 'dual_conflict'
];

var HEADERS_RAWSCANS = [
  'session_id', 'timestamp_ms', 'device_address', 'company_id',
  'rssi', 'was_ibeacon', 'data_hex', 'reject_reason'
];

// ── Sheet helper: open sheet, ensure headers exist ─────────────

function _ensureSheet(spreadsheetId, tabName, headers) {
  var ss = SpreadsheetApp.openById(spreadsheetId);
  var sh = ss.getSheetByName(tabName);
  if (!sh) throw new Error('Tab "' + tabName + '" not found in spreadsheet');

  if (sh.getLastRow() === 0) {
    sh.appendRow(headers);
    sh.getRange(1, 1, 1, headers.length)
      .setFontWeight('bold')
      .setBackground('#f3f3f3');
    SpreadsheetApp.flush();
  }

  return sh;
}

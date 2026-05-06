// ═══════════════════════════════════════════════════════════════
// auth.js — Token validation
// ═══════════════════════════════════════════════════════════════

function validateToken(token) {
  if (!token || typeof token !== 'string') return false;
  return token === _p().BEACONIQ_TOKEN;
}

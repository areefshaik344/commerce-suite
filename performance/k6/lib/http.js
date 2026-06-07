import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './config.js';

export function authedHeaders(token, extra = {}) {
  return {
    headers: {
      'Content-Type':     'application/json',
      'Authorization':    token ? `Bearer ${token}` : '',
      'X-Correlation-Id': `k6-${__VU}-${__ITER}-${Date.now()}`,
      ...extra,
    },
  };
}

export function get(path, token, extra)        { return http.get(`${BASE_URL}${path}`, authedHeaders(token, extra)); }
export function post(path, body, token, extra) { return http.post(`${BASE_URL}${path}`, JSON.stringify(body || {}), authedHeaders(token, extra)); }
export function put(path, body, token, extra)  { return http.put(`${BASE_URL}${path}`, JSON.stringify(body || {}), authedHeaders(token, extra)); }
export function del(path, token, extra)        { return http.del(`${BASE_URL}${path}`, null, authedHeaders(token, extra)); }

export function ok(res, label) {
  return check(res, { [`${label} status 2xx`]: r => r.status >= 200 && r.status < 300 });
}

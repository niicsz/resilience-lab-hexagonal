import http from 'k6/http';
import { check } from 'k6';

export const BASE = __ENV.BASE_URL || 'http://order-service:8080';
export const ADMIN = __ENV.WIREMOCK_ADMIN || 'http://wiremock:8080/__admin';
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export function createOrder() {
  const body = JSON.stringify({ sku: 'SKU-1', quantity: 1, amount: 10.0, currency: 'BRL' });
  const res = http.post(`${BASE}/orders`, body, JSON_HEADERS);
  check(res, { 'order 2xx': (r) => r.status >= 200 && r.status < 300 });
  return res;
}

export function getTracking(id) {
  return http.get(`${BASE}/orders/${id}/tracking`);
}

export function wmReset() {
  return http.post(`${ADMIN}/reset`);
}

export function wmStub(mapping) {
  return http.post(`${ADMIN}/mappings`, JSON.stringify(mapping), JSON_HEADERS);
}

export function wmCount(method, urlPath) {
  const res = http.post(`${ADMIN}/requests/count`,
    JSON.stringify({ method, urlPath }), JSON_HEADERS);
  return res.json('count');
}

import { sleep } from 'k6';
import { createOrder, getTracking, wmReset, wmStub } from './lib/common.js';

export const options = {
  scenarios: {
    checkout: { executor: 'constant-vus', vus: 8, duration: '70s', exec: 'checkout' },
    tracking_flood: { executor: 'constant-vus', vus: 40, duration: '70s', exec: 'trackingFlood', startTime: '10s' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '80s' },
  },
  thresholds: { http_req_failed: ['rate<1'] },
};

function slowTracking(ms) {
  return {
    priority: 1,
    request: { method: 'GET', urlPathPattern: '/tracking/.*' },
    response: {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      jsonBody: { orderPublicId: 'x', carrier: 'ACME', trackingCode: 'XYZ', state: 'IN_TRANSIT' },
      fixedDelayMilliseconds: ms,
    },
  };
}

export function checkout() { createOrder(); sleep(0.3); }
export function trackingFlood() { getTracking(`ord-${__VU}-${__ITER}`); sleep(0.1); }

export function timeline() {
  wmReset();
  sleep(10);
  wmStub(slowTracking(4000));
  sleep(50);
  wmReset();
  sleep(10);
}

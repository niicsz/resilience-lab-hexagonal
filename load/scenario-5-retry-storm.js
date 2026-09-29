import { sleep } from 'k6';
import { createOrder, wmReset, wmStub, wmCount } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 20, duration: '55s', exec: 'load' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '65s' },
  },
  thresholds: { http_req_failed: ['rate<1'] },
};

const paymentOutage = {
  priority: 1,
  request: { method: 'POST', urlPath: '/payments' },
  response: { status: 500 },
};

export function load() { createOrder(); sleep(0.2); }

export function timeline() {
  wmReset();
  sleep(5);
  wmReset();
  wmStub(paymentOutage);
  sleep(40);
  const calls = wmCount('POST', '/payments');
  console.log(`MODE=${__ENV.MODE} outbound POST /payments during outage = ${calls}`);
  wmReset();
  sleep(5);
}

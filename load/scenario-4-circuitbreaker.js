import { sleep } from 'k6';
import { createOrder, wmReset, wmStub } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 20, duration: '90s', exec: 'load' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '100s' },
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
  sleep(15);
  wmStub(paymentOutage);
  sleep(40);
  wmReset();
  sleep(25);
}

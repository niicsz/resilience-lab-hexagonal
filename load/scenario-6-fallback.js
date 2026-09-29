import { sleep } from 'k6';
import { createOrder, wmReset, wmStub } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 15, duration: '60s', exec: 'load' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '70s' },
  },
  thresholds: { http_req_failed: ['rate<1'] },
};

const notificationDown = {
  priority: 1,
  request: { method: 'POST', urlPath: '/notifications' },
  response: { status: 500 },
};

export function load() { createOrder(); sleep(0.3); }

export function timeline() {
  wmReset();
  sleep(15);
  wmStub(notificationDown);
  sleep(30);
  wmReset();
  sleep(15);
}

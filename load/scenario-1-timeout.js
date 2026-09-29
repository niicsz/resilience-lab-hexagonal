import { sleep } from 'k6';
import { createOrder, wmReset, wmStub } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 20, duration: '80s', exec: 'load' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '90s' },
  },
  thresholds: { http_req_failed: ['rate<1'] },
};

function paymentDelay(ms) {
  return {
    priority: 1,
    request: { method: 'POST', urlPath: '/payments' },
    response: {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
      jsonBody: { status: 'APPROVED' },
      fixedDelayMilliseconds: ms,
    },
  };
}

export function load() {
  createOrder();
  sleep(0.3);
}

export function timeline() {
  wmReset();
  sleep(15);
  wmStub(paymentDelay(400));  sleep(15);
  wmStub(paymentDelay(900));  sleep(15);
  wmStub(paymentDelay(2500)); sleep(20);
  wmReset();
  sleep(15);
}

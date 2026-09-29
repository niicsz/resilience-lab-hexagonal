import { sleep } from 'k6';
import { createOrder } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 10, duration: '20s', exec: 'load' },
  },
  thresholds: {
    http_req_failed: ['rate<1'],
  },
};

export function load() {
  createOrder();
  sleep(0.2);
}

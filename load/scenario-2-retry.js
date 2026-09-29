import { sleep } from 'k6';
import { createOrder, wmReset, wmStub } from './lib/common.js';

export const options = {
  scenarios: {
    load: { executor: 'constant-vus', vus: 15, duration: '60s', exec: 'load' },
    timeline: { executor: 'per-vu-iterations', vus: 1, iterations: 1, exec: 'timeline', maxDuration: '70s' },
  },
  thresholds: { http_req_failed: ['rate<1'] },
};

function flakyInventoryStubs() {
  const ok = {
    priority: 1, scenarioName: 'inv-flaky',
    requiredScenarioState: 'Started', newScenarioState: 'down',
    request: { method: 'POST', urlPath: '/inventory/reservations' },
    response: { status: 200, headers: { 'Content-Type': 'application/json' }, jsonBody: { reserved: true } },
  };
  const down = {
    priority: 1, scenarioName: 'inv-flaky',
    requiredScenarioState: 'down', newScenarioState: 'Started',
    request: { method: 'POST', urlPath: '/inventory/reservations' },
    response: { status: 503 },
  };
  return [ok, down];
}

export function load() {
  createOrder();
  sleep(0.3);
}

export function timeline() {
  wmReset();
  sleep(15);
  flakyInventoryStubs().forEach(wmStub);
  sleep(30);
  wmReset();
  sleep(15);
}

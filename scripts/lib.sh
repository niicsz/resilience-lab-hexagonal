#!/usr/bin/env bash

net_name() {
  docker inspect -f '{{range $k, $v := .NetworkSettings.Networks}}{{$k}}{{end}}' \
    "$(docker compose ps -q order-service)"
}

wait_health() {
  for _ in $(seq 1 30); do
    curl -s -m3 localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"' && return 0
    sleep 3
  done
  echo "order-service did not become healthy" >&2
  return 1
}

wm_reset() { curl -s -m5 -XPOST localhost:8081/__admin/reset >/dev/null; }

k6_run() {
  local script="$1"; shift
  docker run --rm -i \
    --network "$(net_name)" \
    -e K6_PROMETHEUS_RW_SERVER_URL=http://prometheus:9090/api/v1/write \
    -e K6_PROMETHEUS_RW_TREND_STATS='p(50),p(95),p(99)' \
    -v "$(pwd)/load:/load:ro" \
    grafana/k6:0.54.0 run -o experimental-prometheus-rw "$@" "/load/${script}"
}

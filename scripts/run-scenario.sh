#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib.sh

N="${1:?usage: run-scenario.sh <1-6> [baseline|resilient|both]}"
MODE="${2:-both}"

script_for() { case "$1" in
  1) echo scenario-1-timeout.js ;;
  2) echo scenario-2-retry.js ;;
  3) echo scenario-3-bulkhead.js ;;
  4) echo scenario-4-circuitbreaker.js ;;
  5) echo scenario-5-retry-storm.js ;;
  6) echo scenario-6-fallback.js ;;
  *) echo "unknown scenario $1" >&2; exit 2 ;;
esac }

baseline_flag_for() { case "$1" in
  1) echo "RESILIENCE_PAYMENT_TIMEOUT_ENABLED=false" ;;
  2) echo "RESILIENCE_INVENTORY_RETRY_ENABLED=false" ;;
  3) echo "RESILIENCE_TRACKING_BULKHEAD_ENABLED=false" ;;
  4) echo "RESILIENCE_PAYMENT_CIRCUITBREAKER_ENABLED=false" ;;
  5) echo "RESILIENCE_PAYMENT_CIRCUITBREAKER_ENABLED=false" ;;
  6) echo "RESILIENCE_NOTIFICATION_FALLBACK_ENABLED=false" ;;
esac }

recreate_app() {
  env $1 docker compose up -d --force-recreate --no-deps order-service
  wait_health
}

run_once() {
  local mode="$1" script; script="$(script_for "$N")"
  local flag=""; [ "$mode" = baseline ] && flag="$(baseline_flag_for "$N")"
  echo "=== scenario $N / $mode  (toggle: ${flag:-all-on}) ==="
  recreate_app "$flag"
  wm_reset
  echo ">>> WINDOW $mode START $(date -u +%FT%TZ)"
  k6_run "$script" -e MODE="$mode" || true
  echo ">>> WINDOW $mode END   $(date -u +%FT%TZ)"
}

case "$MODE" in
  baseline|resilient) run_once "$MODE" ;;
  both) run_once baseline; run_once resilient ;;
  *) echo "mode must be baseline|resilient|both" >&2; exit 2 ;;
esac

recreate_app "" >/dev/null 2>&1 || true
echo "done. Read the marked windows on Grafana (http://localhost:3000)."

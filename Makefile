.PHONY: up down logs smoke scenario-1 scenario-2 scenario-3 scenario-4 scenario-5 scenario-6 all

up:
	docker compose up --build -d
	@echo "app :8080  wiremock-admin :8081/__admin  prometheus :9090  grafana :3000"

down:
	docker compose down

logs:
	docker compose logs -f order-service

smoke:
	bash -c 'source scripts/lib.sh && wait_health && wm_reset && k6_run scenario-0-smoke.js'

scenario-1: ; scripts/run-scenario.sh 1
scenario-2: ; scripts/run-scenario.sh 2
scenario-3: ; scripts/run-scenario.sh 3
scenario-4: ; scripts/run-scenario.sh 4
scenario-5: ; scripts/run-scenario.sh 5
scenario-6: ; scripts/run-scenario.sh 6

all: scenario-1 scenario-2 scenario-3 scenario-4 scenario-5 scenario-6

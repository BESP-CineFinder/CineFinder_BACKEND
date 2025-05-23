#!/bin/bash

# 로그 파일 이름
OUTPUT_FILE="docker_stats_log.csv"

# 모니터링 대상 컨테이너 이름들
CONTAINERS=("backend1" "backend2" "frontend" "CF_nginx")

# CSV 헤더 작성
echo "timestamp,container,name,cpu_perc,mem_usage,mem_limit,mem_perc,net_io,block_io,pids" > "$OUTPUT_FILE"

# 무한 루프: 1초마다 실행
while true; do
  TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

  for CONTAINER in "${CONTAINERS[@]}"; do
    docker stats --no-stream --format \
      "$TIMESTAMP,{{.Container}},{{.Name}},{{.CPUPerc}},{{.MemUsage}},{{.MemPerc}},{{.NetIO}},{{.BlockIO}},{{.PIDs}}" "$CONTAINER" \
      >> "$OUTPUT_FILE"
  done

  sleep 1
done

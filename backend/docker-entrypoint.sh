#!/usr/bin/env sh
set -eu

if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
  db_url="${DATABASE_URL#postgresql://}"
  db_url="${db_url#postgres://}"

  credentials="${db_url%@*}"
  host_and_path="${db_url#*@}"
  host_port="${host_and_path%%/*}"
  db_name_and_query="${host_and_path#*/}"

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_port}/${db_name_and_query}"

  if [ -z "${SPRING_DATASOURCE_USERNAME:-}" ]; then
    export SPRING_DATASOURCE_USERNAME="${credentials%%:*}"
  fi

  if [ -z "${SPRING_DATASOURCE_PASSWORD:-}" ]; then
    password="${credentials#*:}"
    export SPRING_DATASOURCE_PASSWORD="${password}"
  fi
fi

exec "$@"

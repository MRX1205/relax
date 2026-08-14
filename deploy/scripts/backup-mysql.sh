#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${DEPLOY_DIR}/backups"
BACKUP_FILE="${BACKUP_DIR}/relax-$(date +%Y%m%d-%H%M%S).sql.gz"

mkdir -p "${BACKUP_DIR}"

docker compose --project-directory "${DEPLOY_DIR}" exec -T mysql \
  sh -c 'exec mysqldump --single-transaction --routines --triggers -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  | gzip > "${BACKUP_FILE}"

test -s "${BACKUP_FILE}"
echo "Backup created: ${BACKUP_FILE}"


#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <backup.sql.gz>" >&2
  exit 1
fi

BACKUP_FILE="$1"
if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "Backup not found: ${BACKUP_FILE}" >&2
  exit 1
fi

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

gzip -dc "${BACKUP_FILE}" \
  | docker compose --project-directory "${DEPLOY_DIR}" exec -T mysql \
    sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'

echo "Restore completed from: ${BACKUP_FILE}"


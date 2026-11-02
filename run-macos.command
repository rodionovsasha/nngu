#!/bin/bash
set -e
cd "$(dirname "$0")"
if ! command -v java >/dev/null 2>&1; then
  echo "Не найдена Java. Установите JDK 25 и попробуйте снова."
  exit 1
fi
exec java -jar PotentialWellVisualizer.jar

#!/bin/bash
# Сборка всех классов ННГУ из исходников (macOS / Linux, JDK 25).
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

for program in java javac jar; do
  if ! command -v "$program" >/dev/null 2>&1; then
    echo "Ошибка: не найдена команда $program. Установите JDK 25 и проверьте PATH." >&2
    exit 1
  fi
done

echo "Java: $(java -version 2>&1 | head -n 1)"
echo "Компилятор: $(javac -version 2>&1)"

# Чистая сборка: классы находятся только в build/classes.
rm -rf -- "$PROJECT_DIR/build/classes"
mkdir -p "$PROJECT_DIR/build/classes"

javac -encoding UTF-8 -d "$PROJECT_DIR/build/classes" \
  PotentialWellSpectrum.java \
  ElectronPhotonTransition.java \
  PotentialWellVisualizer.java \
  QuantumWellSelfTest.java

echo
printf '%s\n' '=== Проверка физических расчётов ==='
java -cp "$PROJECT_DIR/build/classes" QuantumWellSelfTest

echo
printf '%s\n' '=== Создание исполняемого JAR ==='
jar --create --file "$PROJECT_DIR/PotentialWellVisualizer.jar" \
  --main-class PotentialWellVisualizer \
  -C "$PROJECT_DIR/build/classes" .

echo
printf '%s\n' '=== Проверка экспорта графика в PNG (без GUI) ==='
java -Djava.awt.headless=true -cp "$PROJECT_DIR/build/classes" \
  PotentialWellVisualizer --demo-png "$PROJECT_DIR/build/demo.png"

echo
printf '%s\n' 'Сборка завершена успешно.'
printf 'JAR: %s\n' "$PROJECT_DIR/PotentialWellVisualizer.jar"
printf 'PNG: %s\n' "$PROJECT_DIR/build/demo.png"
printf '%s\n' 'Запуск GUI: java -jar PotentialWellVisualizer.jar'

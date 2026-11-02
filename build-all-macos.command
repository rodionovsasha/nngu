#!/bin/bash
# Чистая сборка Java + проверка Python + сборка VB.NET и тестирование.
# Требуются JDK 25, Python 3 и .NET SDK 8 или новее.
set -euo pipefail
cd "$(dirname "$0")"

echo '=== 1/3 Java: компиляция, тесты, JAR, PNG ==='
bash build-macos.command

echo
echo '=== 2/3 Python: автоматические проверки ==='
if ! command -v python3 >/dev/null 2>&1; then
  echo 'Ошибка: python3 не найден. Установите Python 3.' >&2
  exit 1
fi
python3 -m unittest discover -s python -p 'test_*.py' -v
python3 python/potential_well_visualizer.py --no-open

echo
echo '=== 3/3 Visual Basic.NET: сборка и тесты ==='
if ! command -v dotnet >/dev/null 2>&1; then
  echo 'Ошибка: dotnet не найден. Установите .NET SDK 8 или новее с https://dotnet.microsoft.com/download.' >&2
  echo 'Java и Python уже успешно проверены.' >&2
  exit 1
fi
dotnet build visual-basic/PotentialWellVB.vbproj -c Release
dotnet run --no-build --configuration Release --project visual-basic/PotentialWellVB.vbproj -- test
dotnet run --no-build --configuration Release --project visual-basic/PotentialWellVB.vbproj -- visualize --no-open

echo
echo '=== Всё готово ==='
echo 'Java GUI: java -jar PotentialWellVisualizer.jar'
echo 'Python GUI: python3 python/potential_well_visualizer.py'
echo 'VB.NET GUI: dotnet run --project visual-basic -- visualize'
echo 'HTML GUI: open web/potential-well.html'

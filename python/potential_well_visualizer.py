"""Открывает общую интерактивную HTML-визуализацию в браузере.

Запуск: python3 python/potential_well_visualizer.py [--no-open]
Для браузерных графиков не нужны matplotlib и tkinter.
"""
import argparse
import webbrowser
from pathlib import Path


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Интерактивная визуализация в браузере")
    parser.add_argument("--no-open", action="store_true", help="только вывести путь к файлу")
    args = parser.parse_args(argv)
    page = Path(__file__).resolve().parent.parent / "web" / "potential-well.html"
    if not page.is_file():
        parser.error(f"Не найден файл визуализации: {page}")
    print(f"Визуализация: {page}")
    if not args.no_open and not webbrowser.open(page.as_uri()):
        print("Браузер не открылся автоматически — откройте файл вручную.")


if __name__ == "__main__":
    main()

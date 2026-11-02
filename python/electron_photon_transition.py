"""Задача 2: переход электрона 2 → 1 и фотон.

Запуск: python3 python/electron_photon_transition.py [L_нм]
"""
import argparse
from physics import ELECTRON_VOLT, electron_transition, parse_float


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Энергия электрона и длина волны фотона")
    parser.add_argument("width", nargs="?", type=parse_float, default=0.6, help="ширина ямы, нм")
    p = parser.parse_args(argv)
    try:
        t = electron_transition(p.width)
        print("Задача 2. Электрон в потенциальном ящике: переход 2 → 1")
        print(f"Ширина L = {p.width:.6f} нм\n")
        print(f"E1 (минимальная энергия) = {t.e1_j / ELECTRON_VOLT:.6f} эВ ({t.e1_j:.6e} Дж)")
        print(f"E2                      = {t.e2_j / ELECTRON_VOLT:.6f} эВ ({t.e2_j:.6e} Дж)")
        print(f"ΔE = E2 - E1            = {t.delta_j / ELECTRON_VOLT:.6f} эВ ({t.delta_j:.6e} Дж)")
        print(f"Длина волны фотона λ   = {t.wavelength_nm:.6f} нм")
        print(f"Частота фотона ν       = {t.frequency_hz:.6e} Гц")
    except ValueError as exc:
        parser.error(str(exc))


if __name__ == "__main__":
    main()

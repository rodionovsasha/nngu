"""Задача 1: спектр энергии, ψ_n(x) и |ψ_n(x)|².

Запуск: python3 python/potential_well_spectrum.py [L_нм] [уровни] [m/m_e] [x_нм]
"""
import argparse
from physics import ELECTRON_VOLT, energy_joules, parse_float, probability_density, wave_function


def main(argv: list[str] | None = None) -> None:
    parser = argparse.ArgumentParser(description="Спектр энергии в одномерной потенциальной яме")
    parser.add_argument("width", nargs="?", type=parse_float, default=0.6, help="ширина ямы, нм")
    parser.add_argument("count", nargs="?", type=int, default=5, help="число уровней (1..1000)")
    parser.add_argument("mass", nargs="?", type=parse_float, default=1.0, help="масса в единицах массы электрона")
    parser.add_argument("x", nargs="?", type=parse_float, default=None, help="координата, нм")
    p = parser.parse_args(argv)
    if not 1 <= p.count <= 1000:
        parser.error("Число уровней должно быть в диапазоне 1..1000")
    x = p.x if p.x is not None else p.width / 4
    try:
        print("Задача 1. Бесконечно глубокая потенциальная яма")
        print(f"Ширина L = {p.width:.6f} нм; масса m = {p.mass:.6f} m_e; x = {x:.6f} нм")
        print("  n |   E_n (Дж)    |  E_n (эВ)  | ψ_n(x), нм^-1/2 | |ψ_n(x)|², нм^-1")
        print("----+---------------+------------+-----------------+-----------------")
        for n in range(1, p.count + 1):
            ej = energy_joules(n, p.width, p.mass)
            psi = wave_function(n, x, p.width)
            density = probability_density(n, x, p.width)
            print(f"{n:3d} | {ej:13.6e} | {ej / ELECTRON_VOLT:10.6f} | {psi:15.6f} | {density:15.6f}")
    except ValueError as exc:
        parser.error(str(exc))


if __name__ == "__main__":
    main()

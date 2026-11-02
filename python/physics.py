"""Общие формулы для одномерной бесконечно глубокой потенциальной ямы.

Единицы: геометрия — нм, энергия — Дж внутри расчётов, масса — кг.
Константы согласованы с Java-версией проекта.
"""

import math
from dataclasses import dataclass

PLANCK = 6.62607015e-34  # Дж·с
SPEED_OF_LIGHT = 299_792_458.0  # м/с
ELECTRON_MASS = 9.1093837139e-31  # кг
ELECTRON_VOLT = 1.602176634e-19  # Дж/эВ
NANOMETER = 1e-9  # м


def positive(value: float, name: str) -> float:
    if not math.isfinite(value) or value <= 0.0:
        raise ValueError(f"{name} должно быть конечным положительным числом")
    return value


def energy_joules(level: int, width_nm: float, mass_ratio: float = 1.0) -> float:
    """Энергия E_n = n² h² / (8 m L²), в джоулях."""
    if not isinstance(level, int) or level < 1:
        raise ValueError("Номер уровня n должен быть целым числом >= 1")
    width = positive(width_nm, "Ширина") * NANOMETER
    mass = positive(mass_ratio, "Масса") * ELECTRON_MASS
    energy = level**2 * PLANCK**2 / (8.0 * mass * width**2)
    return positive(energy, "Энергия")


def energy_ev(level: int, width_nm: float, mass_ratio: float = 1.0) -> float:
    return energy_joules(level, width_nm, mass_ratio) / ELECTRON_VOLT


def wave_function(level: int, x_nm: float, width_nm: float) -> float:
    """Нормированная ψ_n(x), единицы — нм^(-1/2)."""
    if not isinstance(level, int) or level < 1 or not math.isfinite(x_nm):
        raise ValueError("Некорректные n или x")
    positive(width_nm, "Ширина")
    if x_nm <= 0.0 or x_nm >= width_nm:
        return 0.0
    return math.sqrt(2.0 / width_nm) * math.sin(level * math.pi * x_nm / width_nm)


def probability_density(level: int, x_nm: float, width_nm: float) -> float:
    """Плотность |ψ|², единицы — нм^(-1)."""
    return wave_function(level, x_nm, width_nm) ** 2


@dataclass(frozen=True)
class PhotonTransition:
    e1_j: float
    e2_j: float
    delta_j: float
    wavelength_nm: float
    frequency_hz: float


def electron_transition(width_nm: float) -> PhotonTransition:
    """Переход электрона 2 → 1; масса всегда равна m_e."""
    e1 = energy_joules(1, width_nm)
    e2 = energy_joules(2, width_nm)
    delta = positive(e2 - e1, "Разность энергий")
    return PhotonTransition(
        e1_j=e1,
        e2_j=e2,
        delta_j=delta,
        wavelength_nm=PLANCK * SPEED_OF_LIGHT / delta / NANOMETER,
        frequency_hz=delta / PLANCK,
    )


def parse_float(value: str) -> float:
    return float(value.replace(",", "."))

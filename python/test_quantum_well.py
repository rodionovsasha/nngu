"""Проверки формул (python3 -m unittest discover -s python -p 'test_*.py' -v)."""
import math
import unittest
from physics import (
    PLANCK, SPEED_OF_LIGHT, ELECTRON_VOLT, energy_joules, energy_ev,
    wave_function, probability_density, electron_transition,
)


class QuantumWellTests(unittest.TestCase):
    def test_first_level(self):
        self.assertAlmostEqual(energy_ev(1, 0.6), 1.044528, delta=2e-6)

    def test_energy_ratios(self):
        self.assertAlmostEqual(energy_ev(2, 0.6) / energy_ev(1, 0.6), 4.0)
        self.assertAlmostEqual(energy_ev(3, 0.6) / energy_ev(1, 0.6), 9.0)

    def test_width_and_mass_scaling(self):
        self.assertAlmostEqual(energy_ev(1, 1.2) / energy_ev(1, 0.6), .25)
        self.assertAlmostEqual(energy_ev(1, 0.6, 2) / energy_ev(1, 0.6), .5)

    def test_wave_boundaries_and_node(self):
        self.assertEqual(wave_function(1, 0, .6), 0.0)
        self.assertEqual(wave_function(1, .6, .6), 0.0)
        self.assertAlmostEqual(wave_function(2, .3, .6), 0.0, places=12)

    def test_density_normalization(self):
        width, n_steps = .6, 20000
        dx = width / n_steps
        total = sum(probability_density(3, i * dx, width) * (0.5 if i in (0, n_steps) else 1)
                    for i in range(n_steps + 1)) * dx
        self.assertAlmostEqual(total, 1.0, delta=1e-8)

    def test_transition(self):
        transition = electron_transition(.6)
        self.assertAlmostEqual(transition.delta_j / ELECTRON_VOLT, 3.133585, delta=3e-6)
        self.assertAlmostEqual(transition.wavelength_nm, 395.663, delta=.002)
        self.assertAlmostEqual(PLANCK * SPEED_OF_LIGHT / transition.delta_j / 1e-9,
                               transition.wavelength_nm)

    def test_validation(self):
        with self.assertRaises(ValueError):
            energy_joules(0, .6)
        with self.assertRaises(ValueError):
            energy_joules(1, -1)
        with self.assertRaises(ValueError):
            wave_function(1, math.nan, .6)


if __name__ == "__main__":
    unittest.main()

"""Регрессия: прямой и пакетный запуск обязаны давать одинаковый результат."""
from pathlib import Path
import subprocess
import sys
import unittest

ROOT = Path(__file__).resolve().parent.parent


class CliTests(unittest.TestCase):
    def test_script_and_package_entry_points(self):
        for module, arguments in (
            ("potential_well_spectrum", ["0.6", "2", "1", "0.15"]),
            ("electron_photon_transition", ["0.6"]),
            ("potential_well_visualizer", ["--no-open"]),
        ):
            with self.subTest(module=module):
                outputs = []
                for entry in ([str(ROOT / "python" / f"{module}.py")], ["-m", f"python.{module}"]):
                    result = subprocess.run([sys.executable, *entry, *arguments], cwd=ROOT,
                                            capture_output=True, text=True, check=True)
                    outputs.append(result.stdout)
                self.assertEqual(*outputs)

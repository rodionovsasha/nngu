"""Единая сборка и проверки для macOS, Linux и Windows; только стандартная библиотека Python."""
from pathlib import Path
import os
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent


def run(*command):
    print("+ " + " ".join(map(str, command)), flush=True)
    # .bat запускается через cmd.exe; аргументы сборки здесь фиксированы.
    if os.name == "nt" and str(command[0]).endswith(".bat"):
        command = (os.environ.get("COMSPEC", "cmd.exe"), "/d", "/c", *command)
    subprocess.run(list(map(str, command)), cwd=ROOT, check=True)


def main():
    os.environ["PYTHONUTF8"] = "1"
    os.environ["PYTHONIOENCODING"] = "utf-8"
    if os.name == "nt":
        sys.stdout.reconfigure(encoding="utf-8")
    for executable in ("javac", "java", "jar", "dotnet", "node"):
        if not shutil.which(executable):
            raise SystemExit(f"Не найден {executable}. Нужны JDK 25, .NET SDK 8+, Node.js 20+ и Python 3.10+.")
    classes = ROOT / "build" / "legacy" / "classes"
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    run("javac", "--release", "25", "-encoding", "UTF-8", "-d", classes,
        "PotentialWellSpectrum.java", "ElectronPhotonTransition.java", "PotentialWellVisualizer.java",
        "QuantumWellSelfTest.java", "tests/SwingInteractionSelfTest.java")
    run("java", "-cp", classes, "QuantumWellSelfTest")
    run("java", "-Djava.awt.headless=true", "-cp", classes, "SwingInteractionSelfTest")
    run("jar", "--create", "--file", "build/legacy/PotentialWellVisualizer.jar", "--main-class", "PotentialWellVisualizer", "-C", classes, ".")
    run("java", "-Djava.awt.headless=true", "-cp", classes, "PotentialWellVisualizer", "--demo-png", "build/legacy/demo.png")
    run("gradlew.bat" if os.name == "nt" else ROOT / "gradlew", "build", "--console=plain")
    run(sys.executable, "-m", "unittest", "discover", "-s", "python", "-p", "test_*.py", "-v")
    run(sys.executable, "-m", "unittest", "python.test_quantum_well", "python.test_cli", "-v")
    run("node", "--test", "tests/web-ui.test.cjs")
    run("dotnet", "build", "visual-basic/PotentialWellVB.vbproj", "-c", "Release")
    for command in (("test",), ("visualize", "--no-open")):
        run("dotnet", "run", "--no-build", "-c", "Release", "--project", "visual-basic/PotentialWellVB.vbproj", "--", *command)
    print("Сборка и проверки всех реализаций завершены.")


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as error:
        raise SystemExit(error.returncode)

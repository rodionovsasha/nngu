import java.util.Locale;

/** Небольшие самопроверки формул без JUnit и внешних зависимостей. */
public final class QuantumWellSelfTest {
    private static final double M_E = 9.1093837139e-31;
    private static final double EV = 1.602176634e-19;
    private static final double H = 6.62607015e-34;
    private static final double C = 299_792_458.0;
    private QuantumWellSelfTest() {}

    private static void equal(String label, double actual, double expected, double tolerance) {
        if (!Double.isFinite(actual) || Math.abs(actual - expected) > tolerance) {
            throw new AssertionError(label + ": got=" + actual + ", expected=" + expected);
        }
        System.out.printf(Locale.US, "OK %-25s %.9f%n", label, actual);
    }

    public static void main(String[] args) {
        final double L = 0.6e-9;
        double e1 = PotentialWellSpectrum.energyJoules(1, M_E, L);
        double e2 = PotentialWellSpectrum.energyJoules(2, M_E, L);
        double e3 = PotentialWellSpectrum.energyJoules(3, M_E, L);
        equal("E1 (эВ)", e1 / EV, 1.044528, 0.000002);
        equal("E2 / E1", e2 / e1, 4.0, 1e-12);
        equal("E3 / E1", e3 / e1, 9.0, 1e-12);
        equal("E1 ∝ 1/L²", PotentialWellSpectrum.energyJoules(1, M_E, 2 * L) / e1, 0.25, 1e-12);
        equal("E1 ∝ 1/m", PotentialWellSpectrum.energyJoules(1, 2 * M_E, L) / e1, 0.5, 1e-12);
        equal("E1 between tasks", ElectronPhotonTransition.energyJoules(1, L) / e1, 1.0, 1e-12);
        equal("ψ(0)", PotentialWellSpectrum.waveFunction(1, 0, 0.6), 0.0, 1e-12);
        equal("ψ(L)", PotentialWellSpectrum.waveFunction(1, 0.6, 0.6), 0.0, 1e-12);
        equal("ψ2(L/2)", PotentialWellSpectrum.waveFunction(2, 0.3, 0.6), 0.0, 1e-12);
        // Численная проверка нормировки: интеграл |ψ|² dx от 0 до L равен единице.
        double dx = 0.6 / 20_000;
        double integral = 0;
        for (int i = 0; i <= 20_000; i++) {
            double f = PotentialWellSpectrum.probabilityDensity(3, i * dx, 0.6);
            integral += (i == 0 || i == 20_000) ? f * dx / 2 : f * dx;
        }
        equal("integral |ψ3|² dx", integral, 1.0, 1e-8);
        double delta = e2 - e1;
        equal("ΔE (эВ)", delta / EV, 3.133585, 0.000003);
        equal("photon λ (нм)", H * C / delta / 1e-9, 395.663, 0.002);
        System.out.println("Все проверки пройдены.");
    }
}

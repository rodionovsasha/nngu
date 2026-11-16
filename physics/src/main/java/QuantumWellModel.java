/** Общая неизменяемая модель двух новых GUI. Все длины — в нанометрах. */
public record QuantumWellModel(double widthNm, double massRatio, int count, int level, double xFraction) {
    public static final double EV = 1.602176634e-19;
    public static final double ELECTRON_MASS = 9.1093837139e-31;
    public static final double PLANCK = 6.62607015e-34;
    public static final double LIGHT_SPEED = 299_792_458.0;

    public QuantumWellModel {
        requireRange("L, нм", widthNm, 0.05, 10);
        requireRange("m/mₑ", massRatio, 0.05, 100);
        if (count < 1 || count > 10) throw new IllegalArgumentException("N: 1..10");
        if (level < 1 || level > count) throw new IllegalArgumentException("n: 1..N (N=" + count + ")");
        requireRange("x/L", xFraction, 0, 1);
    }

    private static void requireRange(String name, double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) {
            throw new IllegalArgumentException(name + ": конечное число в диапазоне " + min + ".." + max);
        }
    }

    public static QuantumWellModel defaults() { return new QuantumWellModel(0.6, 1, 5, 1, 0.25); }
    public double xNm() { return widthNm * xFraction; }
    public double energyEv(int n) {
        return PotentialWellSpectrum.energyJoules(n, massRatio * ELECTRON_MASS, widthNm * 1e-9) / EV;
    }
    public double electronEnergyEv(int n) {
        return electronEnergyJoules(n) / EV;
    }
    public double psi(double fraction) {
        return PotentialWellSpectrum.waveFunction(level, fraction * widthNm, widthNm);
    }
    public double density(double fraction) {
        return PotentialWellSpectrum.probabilityDensity(level, fraction * widthNm, widthNm);
    }
    private double electronEnergyJoules(int n) {
        return ElectronPhotonTransition.energyJoules(n, widthNm * 1e-9);
    }
    public double deltaJoules() { return electronEnergyJoules(2) - electronEnergyJoules(1); }
    public double deltaEv() { return deltaJoules() / EV; }
    public double wavelengthNm() { return PLANCK * LIGHT_SPEED / deltaJoules() * 1e9; }
    public double frequencyHz() { return deltaJoules() / PLANCK; }

    /** Фаза ψ(t) = ψ(0) exp(-iEt/ℏ). Экранное время замедлено, E/E₁ = n². */
    public double phase(double animationSeconds) {
        return animationSeconds * level * level;
    }
}

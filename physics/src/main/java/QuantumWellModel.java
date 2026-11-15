/** Общая неизменяемая модель двух новых GUI. Все длины — в нанометрах. */
public record QuantumWellModel(double widthNm, double massRatio, int count, int level, double xFraction) {
    public static final double EV = 1.602176634e-19;
    public static final double ELECTRON_MASS = 9.1093837139e-31;
    public static final double PLANCK = 6.62607015e-34;
    public static final double LIGHT_SPEED = 299_792_458.0;

    public QuantumWellModel {
        if (!Double.isFinite(widthNm) || widthNm < 0.05 || widthNm > 10
                || !Double.isFinite(massRatio) || massRatio < 0.05 || massRatio > 100
                || count < 1 || count > 10 || level < 1 || level > count
                || !Double.isFinite(xFraction) || xFraction < 0 || xFraction > 1) {
            throw new IllegalArgumentException("L: 0.05..10 нм; m/mₑ: 0.05..100; 1 ≤ n ≤ N ≤ 10; x/L: 0..1");
        }
    }

    public static QuantumWellModel defaults() { return new QuantumWellModel(0.6, 1, 5, 1, 0.25); }
    public double xNm() { return widthNm * xFraction; }
    public double energyEv(int n) {
        return PotentialWellSpectrum.energyJoules(n, massRatio * ELECTRON_MASS, widthNm * 1e-9) / EV;
    }
    public double electronEnergyEv(int n) {
        return ElectronPhotonTransition.energyJoules(n, widthNm * 1e-9) / EV;
    }
    public double psi(double fraction) {
        return PotentialWellSpectrum.waveFunction(level, fraction * widthNm, widthNm);
    }
    public double density(double fraction) {
        return PotentialWellSpectrum.probabilityDensity(level, fraction * widthNm, widthNm);
    }
    public double deltaEv() { return electronEnergyEv(2) - electronEnergyEv(1); }
    public double wavelengthNm() { return PLANCK * LIGHT_SPEED / (deltaEv() * EV) * 1e9; }
    public double frequencyHz() { return deltaEv() * EV / PLANCK; }

    /** Фаза ψ(t) = ψ(0) exp(-iEt/ℏ). Экранное время замедлено, E/E₁ = n². */
    public double phase(double animationSeconds) {
        return animationSeconds * level * level;
    }
}

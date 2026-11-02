import java.util.Locale;

/**
 * Задача № 2. Электрон в бесконечном потенциальном ящике шириной 0,6 нм.
 * Найти E1, E2 - E1 и длину волны фотона при переходе n=2 -> n=1.
 *
 * ННГУ, С. К. Игнатов, «Задачи по квантовой химии», задача 3-7.
 *
 * Запуск:
 *   java ElectronPhotonTransition [ширина_нм]
 * Например:
 *   java ElectronPhotonTransition 0.6
 */
public final class ElectronPhotonTransition {

    private static final double PLANCK = 6.62607015e-34;           // Дж · с
    private static final double ELECTRON_MASS = 9.1093837139e-31;  // кг
    private static final double ELECTRON_VOLT = 1.602176634e-19;   // Дж / эВ
    private static final double SPEED_OF_LIGHT = 299_792_458.0;    // м / с
    private static final double NANOMETER = 1e-9;                   // м

    private ElectronPhotonTransition() {
        // Экземпляры класса не нужны.
    }

    /** Энергия электрона на уровне n (в джоулях). */
    public static double energyJoules(int n, double widthMeters) {
        if (n < 1 || !Double.isFinite(widthMeters) || widthMeters <= 0) {
            throw new IllegalArgumentException("n >= 1 и ширина должна быть > 0");
        }
        return (n * (double) n) * PLANCK * PLANCK
                / (8.0 * ELECTRON_MASS * widthMeters * widthMeters);
    }

    public static void main(String[] args) {
        try {
            if (args.length > 1) {
                throw new IllegalArgumentException("Требуется максимум один аргумент");
            }
            double widthNm = args.length == 0 ? 0.6 : Double.parseDouble(args[0].replace(',', '.'));
            if (!Double.isFinite(widthNm) || widthNm <= 0) {
                throw new IllegalArgumentException("Ширина должна быть положительным числом");
            }

            double widthMeters = widthNm * NANOMETER;

            // Вычисление первого и второго разрешённых уровней энергии.
            double e1 = energyJoules(1, widthMeters);
            double e2 = energyJoules(2, widthMeters);
            double deltaE = e2 - e1;
            if (!Double.isFinite(deltaE) || deltaE <= 0) {
                throw new IllegalArgumentException("Параметр ширины выходит за диапазон double");
            }

            // Энергия фотона равна потере энергии электроном:
            // ΔE = E2 - E1 = h*c/λ, следовательно λ = h*c/ΔE.
            double wavelengthMeters = PLANCK * SPEED_OF_LIGHT / deltaE;
            double frequencyHz = deltaE / PLANCK;

            System.out.println("Задача 2. Электрон в потенциальном ящике: переход 2 -> 1");
            System.out.printf(Locale.US, "Ширина L = %.6f нм%n%n", widthNm);
            System.out.printf(Locale.US, "E1 (минимальная энергия) = %.6f эВ (%.6e Дж)%n",
                    e1 / ELECTRON_VOLT, e1);
            System.out.printf(Locale.US, "E2                      = %.6f эВ (%.6e Дж)%n",
                    e2 / ELECTRON_VOLT, e2);
            System.out.printf(Locale.US, "ΔE = E2 - E1            = %.6f эВ (%.6e Дж)%n",
                    deltaE / ELECTRON_VOLT, deltaE);
            System.out.printf(Locale.US, "Длина волны фотона λ   = %.6f нм%n", wavelengthMeters / NANOMETER);
            System.out.printf(Locale.US, "Частота фотона ν       = %.6e Гц%n", frequencyHz);

            System.out.println("\nПримечание: 4,18 эВ в ответах пособия — это E2,");
            System.out.println("а разность E2 - E1 приблизительно равна 3,13 эВ.");
        } catch (IllegalArgumentException ex) {
            System.err.println("Ошибка: " + ex.getMessage());
            System.err.println("Использование: java ElectronPhotonTransition [L_нм]");
            System.exit(1);
        }
    }
}

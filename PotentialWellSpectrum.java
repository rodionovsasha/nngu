import java.util.Locale;

/**
 * Задача № 1. Частица в одномерной бесконечно глубокой потенциальной яме.
 * Вычисляет разрешённые уровни энергии, волновую функцию и плотность вероятности.
 *
 * ННГУ, Панкратов Е. Л., Булаева Е. А., «Физика», пример 73.
 *
 * Запуск (аргументы необязательны):
 *   java PotentialWellSpectrum [ширина_нм] [число_уровней] [масса_в_массах_электрона] [x_нм]
 * Например:
 *   java PotentialWellSpectrum 0.6 5 1 0.15
 */
public final class PotentialWellSpectrum {

    // Точные значения h и e приняты в Международной системе единиц (СИ).
    private static final double PLANCK = 6.62607015e-34;           // Дж · с
    private static final double ELECTRON_VOLT = 1.602176634e-19;   // Дж / эВ
    private static final double ELECTRON_MASS = 9.1093837139e-31;  // кг
    private static final double NANOMETER = 1e-9;                   // м

    private PotentialWellSpectrum() {
        // Утилитарный класс: экземпляры не нужны.
    }

    /**
     * Энергия n-го уровня в джоулях: E_n = n² h² / (8 m L²).
     * Масса и ширина передаются в единицах СИ.
     */
    public static double energyJoules(int n, double massKg, double widthMeters) {
        if (n < 1 || !isPositiveFinite(massKg) || !isPositiveFinite(widthMeters)) {
            throw new IllegalArgumentException("n >= 1, масса и ширина должны быть положительными");
        }
        double result = (n * (double) n) * PLANCK * PLANCK
                / (8.0 * massKg * widthMeters * widthMeters);
        if (!Double.isFinite(result) || result <= 0) {
            throw new IllegalArgumentException("Параметры выходят за диапазон вычислений double");
        }
        return result;
    }

    /**
     * Нормированная волновая функция ψ_n(x) внутри ямы:
     * ψ_n(x) = sqrt(2/L) * sin(nπx/L).
     *
     * Для удобства x и L указываются в нм, результат — нм^(-1/2).
     * На стенках и за пределами ямы волновая функция равна нулю.
     */
    public static double waveFunction(int n, double xNm, double widthNm) {
        if (n < 1 || !Double.isFinite(xNm) || !isPositiveFinite(widthNm)) {
            throw new IllegalArgumentException("Недопустимые параметры волновой функции");
        }
        if (xNm <= 0.0 || xNm >= widthNm) {
            return 0.0;
        }
        return Math.sqrt(2.0 / widthNm) * Math.sin(n * Math.PI * xNm / widthNm);
    }

    /** Плотность вероятности |ψ_n(x)|², в нм^(-1). */
    public static double probabilityDensity(int n, double xNm, double widthNm) {
        double psi = waveFunction(n, xNm, widthNm);
        return psi * psi;
    }

    public static void main(String[] args) {
        try {
            if (args.length > 4) {
                throw new IllegalArgumentException("Слишком много аргументов");
            }

            // Пример: электрон в яме шириной 0,6 нм, первые пять уровней.
            double widthNm = args.length > 0 ? parseDouble(args[0]) : 0.6;
            int levelCount = args.length > 1 ? Integer.parseInt(args[1]) : 5;
            double massFactor = args.length > 2 ? parseDouble(args[2]) : 1.0;
            double xNm = args.length > 3 ? parseDouble(args[3]) : widthNm / 4.0;

            if (!isPositiveFinite(widthNm) || !isPositiveFinite(massFactor)
                    || !Double.isFinite(xNm) || levelCount < 1 || levelCount > 1000) {
                throw new IllegalArgumentException("Ширина и масса должны быть > 0, число уровней: 1..1000");
            }

            double widthMeters = widthNm * NANOMETER;
            double massKg = massFactor * ELECTRON_MASS;

            System.out.println("Задача 1. Бесконечно глубокая потенциальная яма");
            System.out.printf(Locale.US, "Ширина L = %.6f нм; масса m = %.6f m_e%n",
                    widthNm, massFactor);
            System.out.printf(Locale.US, "Координата x = %.6f нм%n%n", xNm);
            System.out.println("  n |   E_n (Дж)    |  E_n (эВ)  | ψ_n(x), нм^-1/2 | |ψ_n(x)|², нм^-1");
            System.out.println("----+---------------+------------+-----------------+-----------------");

            for (int n = 1; n <= levelCount; n++) {
                double energyJ = energyJoules(n, massKg, widthMeters);
                double energyEv = energyJ / ELECTRON_VOLT;
                double psi = waveFunction(n, xNm, widthNm);
                double density = probabilityDensity(n, xNm, widthNm);

                System.out.printf(Locale.US, "%3d | %13.6e | %10.6f | %15.6f | %15.6f%n",
                        n, energyJ, energyEv, psi, density);
            }

            System.out.println("\nψ_n(x) = sqrt(2/L) * sin(n*pi*x/L), 0 < x < L");
            System.out.println("На стенках (x = 0 и x = L) и за их пределами ψ_n(x) = 0.");
        } catch (IllegalArgumentException ex) {
            System.err.println("Ошибка: " + ex.getMessage());
            System.err.println("Использование: java PotentialWellSpectrum [L_нм] [уровни] [m/m_e] [x_нм]");
            System.exit(1);
        }
    }

    private static double parseDouble(String value) {
        return Double.parseDouble(value.replace(',', '.'));
    }

    private static boolean isPositiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }
}

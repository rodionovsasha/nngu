/** Проверяет границы GUI и независимость задачи о фотоне от эффективной массы. */
public final class QuantumWellModelTest {
    public static void main(String[] args) {
        var reference = QuantumWellModel.defaults();
        var heavy = new QuantumWellModel(0.6, 2, 5, 2, 0.5);
        close(heavy.energyEv(1) / reference.energyEv(1), 0.5);
        close(heavy.deltaEv(), reference.deltaEv());
        close(heavy.wavelengthNm(), reference.wavelengthNm());
        close(heavy.psi(0.5), 0);
        close(heavy.density(0.5), 0);
        close(reference.psi(0), 0);
        close(reference.psi(1), 0);
        close(reference.frequencyHz() * reference.wavelengthNm() * 1e-9, QuantumWellModel.LIGHT_SPEED);
        for (double invalid : new double[] {Double.NaN, Double.POSITIVE_INFINITY, 0, 0.049, 10.01}) {
            reject(() -> new QuantumWellModel(invalid, 1, 5, 1, 0.25));
        }
        reject(() -> new QuantumWellModel(0.6, 1, 1, 2, 0.25));
        reject(() -> new QuantumWellModel(0.6, 1, 5, 1, Double.NaN));
        new QuantumWellModel(0.05, 0.05, 1, 1, 0);
        new QuantumWellModel(10, 100, 10, 10, 1);
        // Каждый кадр сценария должен задавать допустимое состояние GUI.
        for (int index = 0; index < QuantumWellDemo.FRAMES; index++) {
            var frame = QuantumWellDemo.frame(index);
            if (frame.caption().isBlank() || frame.model().level() > frame.model().count()) {
                throw new AssertionError("Некорректный кадр " + index);
            }
        }
        close(QuantumWellDemo.frame(285).seconds(), QuantumWellDemo.frame(314).seconds());
        if (!QuantumWellDemo.frame(315).transition()) throw new AssertionError("Не открыта вкладка перехода");
        close(QuantumWellDemo.frame(375).model().wavelengthNm(), reference.wavelengthNm());
        reject(() -> QuantumWellDemo.frame(-1));
        reject(() -> QuantumWellDemo.frame(QuantumWellDemo.FRAMES));
        System.out.println("Проверки общей модели GUI пройдены.");
    }
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-8 * Math.max(1, Math.abs(expected))) {
            throw new AssertionError(actual + " != " + expected);
        }
    }
    private static void reject(Runnable action) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError("Ожидалась ошибка проверки параметров");
    }
}

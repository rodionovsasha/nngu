/** Общий сценарий записи: 15 секунд на интерфейс, 30 кадров/с, без реального времени. */
public final class QuantumWellDemo {
    public static final int FPS = 30;
    public static final int FRAMES = 15 * FPS;

    private QuantumWellDemo() {}

    public record Frame(QuantumWellModel model, boolean transition, boolean effects,
                        boolean paused, double seconds, String caption) {}

    public static Frame frame(int index) {
        if (index < 0 || index >= FRAMES) throw new IllegalArgumentException("Кадр вне сценария");
        double t = index / (double) FPS;
        int n = t < 1.5 ? 1 : t < 3 ? 2 : 4;
        int count = t < 7.5 ? 5 : t < 8 ? 8 : 3;
        double fraction = t < 4.5 ? 0.25 : t < 6 ? 0.1 + 0.8 * (t - 4.5) / 1.5 : 0.9;
        double width = t < 6 ? 0.6 : t < 7.5 ? 0.6 + 0.6 * (t - 6) / 1.5 : t < 12.5 ? 1.2 : 0.6;
        double mass = t < 7.5 ? 1 : t < 8.5 ? 1 + (t - 7.5) : 2;
        boolean transition = t >= 10.5;
        boolean paused = t >= 9.5 && t < 10.5;
        double seconds = transition ? t - 10.5 : paused ? 9.5 : t;
        String caption = t < 1.5 ? "Стационарное состояние: Re ψ и Im ψ меняются, плотность |ψ|² постоянна"
                : t < 3 ? "Уровень n = 2: узел волновой функции в центре ящика"
                : t < 4.5 ? "Уровень n = 4: больше узлов и выше энергия"
                : t < 6 ? "Меняем координату x: маркеры и значения следуют за выбранной точкой"
                : t < 7.5 ? "Увеличиваем ширину L: энергии уменьшаются как 1/L²"
                : t < 8.5 ? "Меняем массу и число уровней: выбранный n ограничивается автоматически"
                : t < 9 ? "Выключаем свечение и частицы — графики продолжают работать"
                : t < 9.5 ? "Включаем визуальные эффекты"
                : t < 10.5 ? "Пауза: фаза и движение останавливаются"
                : t < 12.5 ? "Переход 2 → 1: электрон теряет энергию и испускает фотон"
                : t < 14 ? "Уменьшаем L: энергия фотона растёт, длина волны уменьшается"
                : "PNG сохранён автоматически: текущая вкладка, параметры и фаза";
        return new Frame(new QuantumWellModel(width, mass, count, Math.min(n, count), fraction),
                transition, !(t >= 8.5 && t < 9), paused, seconds, caption);
    }
}

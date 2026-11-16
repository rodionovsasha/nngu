import javax.swing.*;
import java.lang.reflect.Field;

public final class SwingInteractionSelfTest {
    static void main(String[] args) throws Exception {
        for (int height : new int[] {300, 600, 2400, 8000}) {
            for (int count = 1; count <= 10; count++) {
                double spacing = (height - 81 - 95.0) / count;
                for (int n = 1; n <= count; n++) {
                    double center = height - 81 - (n - 0.5) * spacing;
                    for (double offset : new double[] {-0.35, 0, 0.35}) {
                        int actual = PotentialWellVisualizer.levelAtY((int) Math.round(center + offset * spacing), height, count);
                        if (actual != n) throw new AssertionError("height=" + height + ", n=" + n + ", got=" + actual);
                    }
                }
                if (PotentialWellVisualizer.levelAtY(94, height, count) != -1
                        || PotentialWellVisualizer.levelAtY(height - 80, height, count) != -1) {
                    throw new AssertionError("Клик вне графика выбрал уровень");
                }
            }
        }
        if (args.length > 0 && args[0].equals("--gui")) SwingUtilities.invokeAndWait(() -> {
            try {
                var constructor = PotentialWellVisualizer.class.getDeclaredConstructor();
                constructor.setAccessible(true);
                var ui = constructor.newInstance();
                try {
                    var level = (JSpinner) field(ui, "levelSpinner");
                    var count = (JSpinner) field(ui, "countSpinner");
                    level.setValue(5);
                    count.setValue(1);
                    if (!level.getValue().equals(1)) throw new AssertionError("Спиннер n не ограничен новым N");
                    var settings = field(ui, "settings");
                    var selected = settings.getClass().getDeclaredField("selectedLevel");
                    selected.setAccessible(true);
                    if (selected.getInt(settings) != 1) throw new AssertionError("Модель и спиннер рассинхронизированы");
                } finally { ((JFrame) field(ui, "frame")).dispose(); }
            } catch (ReflectiveOperationException ex) { throw new AssertionError(ex); }
        });
        System.out.println("Проверки выбора уровней Swing пройдены.");
    }
    private static Object field(Object object, String name) throws ReflectiveOperationException {
        Field field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }
}

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/** JavaFX 27 / Java 25: интерактивный Canvas, свечение и анимация комплексной фазы. */
public final class QuantumWellFxApp extends Application {
    private static final Color CYAN = Color.web("#66e4ff");
    private static final Color MINT = Color.web("#6befbb");
    private static final Color GOLD = Color.web("#ffce82");
    private static final Color MUTED = Color.web("#94a7c5");
    private QuantumWellModel model = QuantumWellModel.defaults();
    private final Spinner<Double> width = new Spinner<>(0.05, 10, 0.6, 0.05);
    private final Spinner<Double> mass = new Spinner<>(0.05, 100, 1, 0.05);
    private final Spinner<Integer> count = new Spinner<>(1, 10, 5);
    private final Spinner<Integer> level = new Spinner<>(1, 5, 1);
    private final Slider position = new Slider(0, 1, 0.25);
    private final CheckBox effects = new CheckBox("Свечение и частицы");
    private final Label coordinate = new Label();
    private final Label hint = new Label();
    private final Label[] values = {new Label(), new Label(), new Label(), new Label()};
    private final Label[] names = {new Label(), new Label(), new Label(), new Label()};
    private final Plot plot = new Plot();
    private boolean transition;
    private boolean paused;
    private boolean updating;
    private double seconds;
    private AnimationTimer timer;
    private BorderPane root;
    private Stage stage;

    public static void main(String[] args) { launch(args); }

    @Override public void start(Stage stage) {
        this.stage = stage;
        transition = getParameters().getRaw().contains("--transition");
        root = new BorderPane();
        root.setPadding(new Insets(26));
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #080f20, #15213a);");
        var title = new Label("Квантовая лаборатория");
        title.setStyle("-fx-font-size: 30; -fx-font-weight: bold; -fx-text-fill: #eef6ff;");
        var subtitle = new Label("ННГУ  /  ЧАСТИЦА В ПОТЕНЦИАЛЬНОМ ЯЩИКЕ");
        subtitle.setStyle("-fx-text-fill: #66e4ff; -fx-font-size: 11;");
        var badge = new Label("JavaFX 27  ·  Java 25");
        badge.getStyleClass().add("badge");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var heading = new HBox(18, new VBox(7, subtitle, title), spacer, badge);
        heading.setAlignment(Pos.CENTER_LEFT);
        heading.setPadding(new Insets(0, 0, 24, 0));
        root.setTop(heading);

        effects.setSelected(true);
        var pause = new Button("Ⅱ  Пауза");
        pause.setMaxWidth(Double.MAX_VALUE);
        pause.setOnAction(e -> {
            paused = !paused;
            pause.setText(paused ? "▶  Продолжить" : "Ⅱ  Пауза");
        });
        var reset = new Button("Сбросить параметры");
        reset.setMaxWidth(Double.MAX_VALUE);
        reset.setOnAction(e -> {
            updating = true;
            width.getValueFactory().setValue(0.6);
            mass.getValueFactory().setValue(1.0);
            count.getValueFactory().setValue(5);
            level.getValueFactory().setValue(1);
            position.setValue(0.25);
            updating = false;
            seconds = 0;
            update();
        });
        var export = new Button("↓  Сохранить PNG");
        export.getStyleClass().add("primary");
        export.setMaxWidth(Double.MAX_VALUE);
        export.setOnAction(e -> export());
        var note = new Label("Масса меняет только спектр.\nВ переходе 2 → 1 используется mₑ.\n\nФаза замедлена: E/E₁ = n².\nПлотность стационарна.\n\nПоложение уровней и движение\nфотона показаны схематично.");
        note.setWrapText(true);
        note.setStyle("-fx-font-size: 11; -fx-text-fill: #94a7c5;");
        var controls = new VBox(15,
                small("ПАРАМЕТРЫ МОДЕЛИ"), field("Ширина L, нм", width), field("Масса m / mₑ", mass),
                field("Число уровней N", count), field("Выбранный уровень n", level),
                new VBox(6, coordinate, position), new Separator(), effects, pause, reset, export, note);
        controls.getStyleClass().add("card");
        controls.setPrefWidth(240);
        controls.setMinWidth(240);
        var scroll = new ScrollPane(controls);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(244);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        BorderPane.setMargin(scroll, new Insets(0, 22, 0, 0));
        root.setLeft(scroll);

        var spectrum = new ToggleButton("01  Спектр и состояние");
        var photon = new ToggleButton("02  Переход и фотон");
        var tabs = new ToggleGroup();
        spectrum.setToggleGroup(tabs);
        photon.setToggleGroup(tabs);
        (transition ? photon : spectrum).setSelected(true);
        tabs.selectedToggleProperty().addListener((_, old, selected) -> {
            if (selected == null) { old.setSelected(true); return; }
            transition = selected == photon;
            update();
        });
        var metrics = new HBox(12);
        for (int i = 0; i < values.length; i++) {
            values[i].setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #eef6ff;");
            names[i].setStyle("-fx-text-fill: #94a7c5; -fx-font-size: 11;");
            var metric = new VBox(8, names[i], values[i]);
            metric.getStyleClass().add("card");
            metric.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(metric, Priority.ALWAYS);
            metrics.getChildren().add(metric);
        }
        hint.setWrapText(true);
        hint.setMinHeight(38);
        hint.setStyle("-fx-font-size: 11; -fx-text-fill: #94a7c5;");
        var content = new VBox(16, new HBox(10, spectrum, photon), metrics, plot, hint);
        VBox.setVgrow(plot, Priority.ALWAYS);
        root.setCenter(content);
        var scene = new Scene(root, 1360, 880);
        scene.getStylesheets().add(getClass().getResource("quantum.css").toExternalForm());
        stage.setTitle("ННГУ · Квантовая лаборатория · JavaFX 27");
        stage.setMinWidth(1100);
        stage.setMinHeight(780);
        stage.setScene(scene);
        width.valueProperty().addListener((_, _, _) -> update());
        mass.valueProperty().addListener((_, _, _) -> update());
        count.valueProperty().addListener((_, _, _) -> update());
        level.valueProperty().addListener((_, _, _) -> update());
        position.valueProperty().addListener((_, _, _) -> update());
        effects.selectedProperty().addListener((_, _, _) -> plot.draw());
        update();
        stage.show();
        timer = new AnimationTimer() {
            private long previous;
            @Override public void handle(long now) {
                if (previous != 0 && !paused) {
                    seconds += Math.min(0.1, (now - previous) / 1e9);
                    plot.draw();
                }
                previous = now;
            }
        };
        var args = getParameters().getRaw();
        if (args.contains("--demo-png")) {
            int index = args.indexOf("--demo-png");
            if (index + 1 >= args.size()) throw new IllegalArgumentException("--demo-png требует путь к PNG");
            seconds = transition ? 2.7 : 0.7;
            Platform.runLater(() -> {
                try { save(new File(args.get(index + 1))); }
                catch (IOException ex) { throw new java.io.UncheckedIOException(ex); }
                finally { Platform.exit(); }
            });
        } else timer.start();
    }

    @Override public void stop() { if (timer != null) timer.stop(); }

    private void update() {
        if (updating) return;
        updating = true;
        var levels = (SpinnerValueFactory.IntegerSpinnerValueFactory) level.getValueFactory();
        levels.setMax(count.getValue());
        levels.setValue(Math.min(level.getValue(), count.getValue()));
        model = new QuantumWellModel(width.getValue(), mass.getValue(), count.getValue(), level.getValue(), position.getValue());
        updating = false;
        coordinate.setText(fmt("Координата x = %.3f нм", model.xNm()));
        if (transition) {
            metric(0, "ЭНЕРГИЯ E₁", fmt("%.4f эВ", model.electronEnergyEv(1)));
            metric(1, "ЭНЕРГИЯ E₂", fmt("%.4f эВ", model.electronEnergyEv(2)));
            metric(2, "ФОТОН ΔE", fmt("%.4f эВ", model.deltaEv()));
            metric(3, "ДЛИНА ВОЛНЫ λ", fmt("%.2f нм", model.wavelengthNm()));
            hint.setText(fmt("ΔE = hν = hc/λ  ·  ν = %.5e Гц  ·  m = mₑ  ·  цикл анимации условный, время не физическое", model.frequencyHz()));
        } else {
            metric(0, "ЭНЕРГИЯ Eₙ", fmt("%.4f эВ", model.energyEv(model.level())));
            metric(1, "ψₙ(x, 0), нм⁻½", fmt("%+.4f", model.psi(model.xFraction())));
            metric(2, "|ψₙ(x)|², нм⁻¹", fmt("%.4f", model.density(model.xFraction())));
            metric(3, "СОСТОЯНИЕ", "n = " + model.level());
            hint.setText("Выберите n на спектре и x на графике.  Re ψ = ψ₀ cos φ; Im ψ = −ψ₀ sin φ (пунктир); |ψ|² стационарна.");
        }
        plot.draw();
    }

    private void metric(int i, String name, String value) { names[i].setText(name); values[i].setText(value); }
    private static String fmt(String pattern, Object... args) { return String.format(Locale.US, pattern, args); }
    private static Label small(String text) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: #66e4ff; -fx-font-size: 11; -fx-font-weight: bold;");
        return label;
    }
    private static VBox field(String title, Control control) { return new VBox(7, new Label(title), control); }

    private void export() {
        var chooser = new FileChooser();
        chooser.setTitle("Сохранить визуализацию");
        chooser.setInitialFileName(transition ? "javafx-transition.png" : "javafx-spectrum.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        var output = chooser.showSaveDialog(stage);
        if (output == null) return;
        if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".png")) output = new File(output + ".png");
        try { save(output); hint.setText("Сохранено: " + output.getAbsolutePath()); }
        catch (IOException ex) { new Alert(Alert.AlertType.ERROR, "Не удалось сохранить PNG: " + ex.getMessage()).showAndWait(); }
    }

    private void save(File output) throws IOException {
        root.applyCss();
        root.layout();
        plot.draw();
        WritableImage image = root.snapshot(null, null);
        var buffered = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        image.getPixelReader().getPixels(0, 0, buffered.getWidth(), buffered.getHeight(),
                javafx.scene.image.PixelFormat.getIntArgbInstance(),
                ((java.awt.image.DataBufferInt) buffered.getRaster().getDataBuffer()).getData(), 0, buffered.getWidth());
        var parent = output.getAbsoluteFile().getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) throw new IOException("Не удалось создать " + parent);
        if (!ImageIO.write(buffered, "png", output)) throw new IOException("PNG encoder недоступен");
        System.out.println("PNG: " + output.getAbsolutePath());
    }

    private final class Plot extends Region {
        private final Canvas canvas = new Canvas();
        Plot() {
            getChildren().add(canvas);
            setMinHeight(360);
            canvas.setOnMousePressed(e -> select(e.getX(), e.getY()));
            canvas.setOnMouseDragged(e -> select(e.getX(), e.getY()));
        }
        private void select(double x, double y) {
            if (transition) return;
            x = x / canvas.getWidth() * 1000;
            y = y / canvas.getHeight() * 560;
            if (x > 40 && x < 310 && y > 78 && y < 480) {
                int n = (int) Math.round((470 - y) / 370 * Math.max(1, model.count() - 1)) + 1;
                level.getValueFactory().setValue(Math.clamp(n, 1, model.count()));
            } else if (x >= 400 && x <= 950 && y >= 65 && y <= 490) {
                position.setValue(Math.clamp((x - 400) / 550, 0, 1));
            }
        }
        @Override protected void layoutChildren() {
            canvas.setWidth(getWidth());
            canvas.setHeight(getHeight());
            draw();
        }
        void draw() {
            var g = canvas.getGraphicsContext2D();
            g.setTransform(1, 0, 0, 1, 0, 0);
            g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            g.scale(canvas.getWidth() / 1000, canvas.getHeight() / 560);
            g.setFill(Color.web("#101d33"));
            g.fillRoundRect(0, 0, 1000, 560, 24, 24);
            if (effects.isSelected()) {
                g.setGlobalBlendMode(BlendMode.ADD);
                for (int i = 0; i < 36; i++) {
                    double x = (i * 137.3 + seconds * (4 + i % 4)) % 1000;
                    double y = (i * 83.7) % 560;
                    g.setFill(Color.color(0.25, 0.65, 1, 0.10));
                    g.fillOval(x, y, 2 + i % 3, 2 + i % 3);
                }
                g.setGlobalBlendMode(BlendMode.SRC_OVER);
            }
            if (transition) drawTransition(g); else drawSpectrum(g);
        }
        private void text(GraphicsContext g, String text, double x, double y, Color color, double size) {
            g.setEffect(null);
            g.setFill(color);
            g.setFont(Font.font("System", size));
            g.fillText(text, x, y);
        }
        private void line(GraphicsContext g, double x1, double y1, double x2, double y2, Color color, double width) {
            g.setStroke(color);
            g.setLineWidth(width);
            g.strokeLine(x1, y1, x2, y2);
        }
        private void glow(GraphicsContext g, Color color) {
            g.setEffect(effects.isSelected() ? new DropShadow(18, color.deriveColor(0, 1, 1, 0.7)) : null);
        }
        private void drawSpectrum(GraphicsContext g) {
            text(g, "ЭНЕРГЕТИЧЕСКИЙ СПЕКТР", 30, 38, MUTED, 12);
            text(g, "ψₙ(x, t)  ·  нм⁻½", 380, 38, CYAN, 15);
            text(g, "Re —   /   Im ⋯", 785, 38, Color.web("#c1acff"), 12);
            text(g, "|ψₙ(x)|²  ·  нм⁻¹", 380, 302, MINT, 15);
            line(g, 335, 24, 335, 530, Color.web("#26364d"), 1);
            for (int n = 1; n <= model.count(); n++) {
                double y = 470 - (n - 1.0) / Math.max(1, model.count() - 1) * 370;
                boolean selected = n == model.level();
                glow(g, selected ? CYAN : Color.TRANSPARENT);
                line(g, 40, y, 305, y, selected ? CYAN : Color.web("#3c526e"), selected ? 3 : 1);
                text(g, fmt("n=%d     %.4f эВ", n, model.energyEv(n)), 50, y - 11, selected ? CYAN : MUTED, 13);
            }
            text(g, "Eₙ = n²h² / (8mL²)", 40, 520, MUTED, 13);
            curve(g, false, 65, 248);
            curve(g, true, 328, 503);
        }
        private void curve(GraphicsContext g, boolean density, double top, double bottom) {
            double left = 400, right = 950, base = density ? bottom : (top + bottom) / 2;
            double amplitude = density ? (bottom - top) * 0.85 : (bottom - top) * 0.41;
            for (int i = 0; i <= 4; i++) {
                double x = left + (right - left) * i / 4;
                line(g, x, top, x, bottom, Color.web("#24334a"), 1);
            }
            line(g, left, base, right, base, Color.web("#435671"), 1);
            Color color = density ? MINT : CYAN;
            double phase = Math.cos(model.phase(seconds));
            // Заполнение строится по текущей кривой; |ψ|² не зависит от фазы.
            g.beginPath();
            g.moveTo(left, base);
            for (int i = 0; i <= 400; i++) {
                double t = i / 400.0, sine = Math.sin(model.level() * Math.PI * t);
                g.lineTo(left + t * (right - left), base - amplitude * (density ? sine * sine : sine * phase));
            }
            g.lineTo(right, base);
            g.closePath();
            g.setFill(new LinearGradient(0, top, 0, bottom, false, CycleMethod.NO_CYCLE,
                    new Stop(0, color.deriveColor(0, 1, 1, 0.28)), new Stop(1, Color.TRANSPARENT)));
            g.fill();
            if (!density) {
                g.beginPath();
                for (int i = 0; i <= 400; i++) {
                    double t = i / 400.0;
                    double y = base + amplitude * Math.sin(model.level() * Math.PI * t) * Math.sin(model.phase(seconds));
                    if (i == 0) g.moveTo(left, y); else g.lineTo(left + t * (right - left), y);
                }
                g.setStroke(Color.web("#c1acff"));
                g.setLineWidth(1.5);
                g.setLineDashes(6, 5);
                g.stroke();
                g.setLineDashes();
            }
            g.beginPath();
            for (int i = 0; i <= 400; i++) {
                double t = i / 400.0, sine = Math.sin(model.level() * Math.PI * t);
                double y = base - amplitude * (density ? sine * sine : sine * phase);
                if (i == 0) g.moveTo(left, y); else g.lineTo(left + t * (right - left), y);
            }
            glow(g, color);
            g.setStroke(color);
            g.setLineWidth(2.5);
            g.stroke();
            g.setEffect(null);
            double x = left + model.xFraction() * (right - left);
            double sine = Math.sin(model.level() * Math.PI * model.xFraction());
            double y = base - amplitude * (density ? sine * sine : sine * phase);
            g.setLineDashes(4, 5);
            line(g, x, top, x, bottom, GOLD.deriveColor(0, 1, 1, 0.6), 1);
            g.setLineDashes();
            glow(g, GOLD);
            g.setFill(GOLD);
            g.fillOval(x - 5, y - 5, 10, 10);
            double limit = density ? 2 / model.widthNm() : Math.sqrt(2 / model.widthNm());
            text(g, fmt("%.2f", limit), 350, top + 12, MUTED, 11);
            text(g, "0", left, bottom + 20, MUTED, 11);
            text(g, fmt("L = %.2f нм", model.widthNm()), right - 85, bottom + 20, MUTED, 11);
        }
        private void drawTransition(GraphicsContext g) {
            text(g, "ИСПУСКАНИЕ ФОТОНА  /  2 → 1", 32, 40, MUTED, 13);
            text(g, "Электрон отдаёт разность энергий электромагнитному полю", 32, 68, MUTED, 13);
            double cycle = seconds % 4 / 4;
            for (int n = 1; n <= 2; n++) {
                double y = n == 2 ? 165 : 405;
                glow(g, n == 2 ? CYAN : MINT);
                line(g, 85, y, 490, y, n == 2 ? CYAN : MINT, 2.5);
                text(g, fmt("E%d = %.6f эВ", n, model.electronEnergyEv(n)), 95, y - 22, n == 2 ? CYAN : MINT, 19);
            }
            line(g, 280, 190, 280, 375, GOLD, 2);
            line(g, 280, 375, 271, 360, GOLD, 2);
            line(g, 280, 375, 289, 360, GOLD, 2);
            text(g, fmt("ΔE = %.6f эВ", model.deltaEv()), 300, 294, GOLD, 17);
            double fall = Math.clamp(cycle / 0.4, 0, 1);
            fall = fall * fall * (3 - 2 * fall);
            glow(g, CYAN);
            g.setFill(Color.WHITE);
            g.fillOval(269, 154 + 240 * fall, 22, 22);
            if (cycle > 0.4) {
                double travel = (cycle - 0.4) / 0.6;
                double center = 310 + travel * 580;
                g.beginPath();
                for (int i = 0; i <= 160; i++) {
                    double t = i / 160.0;
                    double x = center - 80 + 160 * t;
                    double y = 405 + Math.sin(t * Math.PI * 8 - seconds * 8) * Math.sin(t * Math.PI) * 22;
                    if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
                }
                glow(g, GOLD);
                g.setStroke(GOLD);
                g.setLineWidth(3);
                g.stroke();
            }
            text(g, "ФОТОН", 650, 160, GOLD, 13);
            text(g, fmt("λ = %.3f нм", model.wavelengthNm()), 650, 206, Color.WHITE, 28);
            text(g, fmt("ν = %.4e Гц", model.frequencyHz()), 650, 244, MUTED, 16);
            text(g, "ΔE = hν = hc/λ", 650, 292, CYAN, 21);
            text(g, "Ширина ящика определяет длину волны и энергию фотона", 85, 510, MUTED, 14);
        }
    }
}

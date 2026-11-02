import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Интерактивная учебная модель бесконечно глубокой одномерной потенциальной ямы.
 *
 * Оба расчёта используют формулы исходных консольных классов:
 *   PotentialWellSpectrum      — спектр, волновые функции, плотности вероятности;
 *   ElectronPhotonTransition   — переход электрона 2 → 1 и энергия фотона.
 *
 * Только Java Swing/Java2D, без внешних зависимостей. Совместимо с Java 25.
 * Запуск: java PotentialWellVisualizer
 * Экспорт примера без GUI: java PotentialWellVisualizer --demo-png preview.png
 */
public final class PotentialWellVisualizer {
    private static final Color NAVY = new Color(29, 46, 78);
    private static final Color BLUE = new Color(36, 105, 210);
    private static final Color TEAL = new Color(12, 152, 144);
    private static final Color ORANGE = new Color(234, 127, 38);
    private static final Color SLATE = new Color(96, 115, 140);
    private static final Color GRID = new Color(224, 231, 239);
    private static final Color PALE = new Color(243, 247, 252);
    private static final double ELECTRON_VOLT = 1.602176634e-19;
    private static final double ELECTRON_MASS = 9.1093837139e-31;
    private static final double NANOMETER = 1e-9;
    private static final double PLANCK = 6.62607015e-34;
    private static final double SPEED_OF_LIGHT = 299_792_458.0;

    /** Общее состояние первой и второй вкладок; ширина L одинаковая. */
    private static final class Settings {
        double widthNm = 0.6;
        double massRatio = 1.0;
        int count = 5;
        int selectedLevel = 1;
        double xFraction = 0.25;

        double energyEv(int level) {
            return PotentialWellSpectrum.energyJoules(
                    level, massRatio * ELECTRON_MASS, widthNm * NANOMETER) / ELECTRON_VOLT;
        }

        double electronEnergyEv(int level) {
            return ElectronPhotonTransition.energyJoules(level, widthNm * NANOMETER) / ELECTRON_VOLT;
        }

        double wavelengthNm() {
            double energyDifferenceJ = ElectronPhotonTransition.energyJoules(2, widthNm * NANOMETER)
                    - ElectronPhotonTransition.energyJoules(1, widthNm * NANOMETER);
            return PLANCK * SPEED_OF_LIGHT / energyDifferenceJ / NANOMETER;
        }
    }

    private final Settings settings = new Settings();
    private final JFrame frame = new JFrame("ННГУ · Электрон в потенциальном ящике");
    private final JTabbedPane tabs = new JTabbedPane();
    private final EnergyLevelsPanel levelsPanel = new EnergyLevelsPanel();
    private final CurvePanel wavePanel = new CurvePanel(false);
    private final CurvePanel densityPanel = new CurvePanel(true);
    private final TransitionPanel transitionPanel = new TransitionPanel();
    private final JLabel stateLabel = new JLabel();
    private final JLabel transitionHelp = new JLabel();
    private final JLabel coordinateLabel = new JLabel();
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(0.6, 0.05, 10.0, 0.05));
    private final JSpinner massSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.05, 100.0, 0.05));
    private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 10, 1));
    private final JSpinner levelSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    private final JSlider positionSlider = new JSlider(0, 1000, 250);

    private PotentialWellVisualizer() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(940, 640));
        frame.setSize(1210, 810);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout(0, 0));
        frame.getContentPane().setBackground(PALE);

        JPanel header = new JPanel(new BorderLayout(10, 12));
        header.setBorder(new EmptyBorder(19, 22, 14, 22));
        header.setBackground(PALE);
        JLabel title = new JLabel("Частица в потенциальном ящике");
        title.setForeground(NAVY);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 23f));
        JLabel subtitle = new JLabel("ННГУ · квантовая механика · бесконечно глубокая одномерная потенциальная яма");
        subtitle.setForeground(SLATE);
        JPanel headings = new JPanel(new GridLayout(2, 1, 0, 2));
        headings.setOpaque(false);
        headings.add(title);
        headings.add(subtitle);
        header.add(headings, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        controls.setBackground(Color.WHITE);
        controls.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GRID, 1), new EmptyBorder(5, 10, 5, 10)));
        controls.add(field("Ширина L, нм", widthSpinner));
        controls.add(field("Масса m / mₑ¹", massSpinner));
        controls.add(field("Уровней", countSpinner));
        controls.add(field("Выбран n", levelSpinner));
        positionSlider.setPreferredSize(new Dimension(135, 24));
        positionSlider.setOpaque(false);
        JPanel position = new JPanel(new BorderLayout(0, 1));
        position.setOpaque(false);
        coordinateLabel.setFont(coordinateLabel.getFont().deriveFont(Font.PLAIN, 11f));
        position.add(coordinateLabel, BorderLayout.NORTH);
        position.add(positionSlider, BorderLayout.SOUTH);
        controls.add(position);
        header.add(controls, BorderLayout.SOUTH);
        frame.add(header, BorderLayout.NORTH);

        JPanel spectrumView = new JPanel(new BorderLayout(10, 10));
        spectrumView.setBackground(PALE);
        spectrumView.setBorder(new EmptyBorder(10, 13, 11, 13));
        JPanel rightPlots = new JPanel(new GridLayout(2, 1, 0, 10));
        rightPlots.setOpaque(false);
        rightPlots.add(wavePanel);
        rightPlots.add(densityPanel);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, levelsPanel, rightPlots);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.37);
        split.setContinuousLayout(true);
        spectrumView.add(split, BorderLayout.CENTER);
        stateLabel.setForeground(NAVY);
        stateLabel.setBorder(new EmptyBorder(8, 7, 5, 0));
        spectrumView.add(stateLabel, BorderLayout.SOUTH);

        JPanel transitionView = new JPanel(new BorderLayout(14, 10));
        transitionView.setBackground(PALE);
        transitionView.setBorder(new EmptyBorder(10, 13, 11, 13));
        transitionView.add(transitionPanel, BorderLayout.CENTER);
        transitionHelp.setForeground(NAVY);
        transitionHelp.setBorder(new EmptyBorder(8, 7, 6, 0));
        transitionView.add(transitionHelp, BorderLayout.SOUTH);

        tabs.addTab("1 · Уровни, ψ(x), |ψ(x)|²", spectrumView);
        tabs.addTab("2 · Переход 2 → 1 и фотон", transitionView);
        tabs.setFont(tabs.getFont().deriveFont(Font.BOLD, 14f));
        frame.add(tabs, BorderLayout.CENTER);

        JButton export = new JButton("Сохранить график в PNG");
        export.addActionListener(e -> exportActiveTab());
        JButton reset = new JButton("Сбросить параметры");
        reset.addActionListener(e -> reset());
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(PALE);
        footer.setBorder(new EmptyBorder(9, 22, 15, 22));
        JLabel hint = new JLabel("¹ Масса относится только к первой задаче. Во второй всегда используется масса электрона.");
        hint.setForeground(SLATE);
        hint.setFont(hint.getFont().deriveFont(11f));
        footer.add(hint, BorderLayout.WEST);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 9, 0));
        buttons.setOpaque(false);
        buttons.add(reset);
        buttons.add(export);
        footer.add(buttons, BorderLayout.EAST);
        frame.add(footer, BorderLayout.SOUTH);

        widthSpinner.addChangeListener(e -> update());
        massSpinner.addChangeListener(e -> update());
        countSpinner.addChangeListener(e -> update());
        levelSpinner.addChangeListener(e -> update());
        positionSlider.addChangeListener(e -> update());
        update();
    }

    private JPanel field(String label, JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(82, 27));
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(l.getFont().deriveFont(11f));
        p.add(l, BorderLayout.NORTH);
        p.add(spinner, BorderLayout.SOUTH);
        return p;
    }

    private void update() {
        settings.widthNm = ((Number) widthSpinner.getValue()).doubleValue();
        settings.massRatio = ((Number) massSpinner.getValue()).doubleValue();
        settings.count = ((Number) countSpinner.getValue()).intValue();
        SpinnerNumberModel model = (SpinnerNumberModel) levelSpinner.getModel();
        model.setMaximum(settings.count);
        settings.selectedLevel = ((Number) levelSpinner.getValue()).intValue();
        settings.xFraction = positionSlider.getValue() / 1000.0;
        double x = settings.widthNm * settings.xFraction;
        int n = settings.selectedLevel;
        double psi = PotentialWellSpectrum.waveFunction(n, x, settings.widthNm);
        double density = PotentialWellSpectrum.probabilityDensity(n, x, settings.widthNm);
        coordinateLabel.setText(String.format(Locale.US, "Координата x = %.3f нм", x));
        stateLabel.setText(String.format(Locale.US,
                "Уровень n=%d: Eₙ = %.6f эВ    •    ψₙ(x) = %.5f нм⁻½    •    |ψₙ(x)|² = %.5f нм⁻¹",
                n, settings.energyEv(n), psi, density));
        double e1 = settings.electronEnergyEv(1);
        double e2 = settings.electronEnergyEv(2);
        transitionHelp.setText(String.format(Locale.US,
                "E₁ = %.6f эВ    •    E₂ = %.6f эВ    •    ΔE = %.6f эВ    •    λ = %.3f нм",
                e1, e2, e2 - e1, settings.wavelengthNm()));
        levelsPanel.repaint();
        wavePanel.repaint();
        densityPanel.repaint();
        transitionPanel.repaint();
    }

    private void reset() {
        widthSpinner.setValue(0.6);
        massSpinner.setValue(1.0);
        countSpinner.setValue(5);
        levelSpinner.setValue(1);
        positionSlider.setValue(250);
        tabs.setSelectedIndex(0);
    }

    private void exportActiveTab() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(tabs.getSelectedIndex() == 0 ? "potential-well-spectrum.png" : "electron-photon-transition.png"));
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) return;
        File output = chooser.getSelectedFile();
        if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".png")) {
            output = new File(output.getParentFile(), output.getName() + ".png");
        }
        if (output.exists()) {
            int choice = JOptionPane.showConfirmDialog(frame, "Перезаписать существующий файл?", "Подтверждение",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) return;
        }
        try {
            saveImage(tabs.getSelectedComponent(), output);
            JOptionPane.showMessageDialog(frame, "График сохранён:\n" + output.getAbsolutePath());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(frame, "Не удалось сохранить файл: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void saveImage(Component component, File output) throws IOException {
        if (component.getWidth() < 10 || component.getHeight() < 10) {
            throw new IOException("Недоступен размер области графика");
        }
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            component.printAll(g);
        } finally {
            g.dispose();
        }
        if (!ImageIO.write(image, "png", output)) throw new IOException("Нет кодировщика PNG");
    }

    private static void setup(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(1.5f));
    }

    private static void title(Graphics2D g, String text, int left) {
        g.setColor(NAVY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(text, left, 27);
    }

    private static void lightLabel(Graphics2D g, String text, int x, int y) {
        g.setColor(SLATE);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(text, x, y);
    }

    private static abstract class WhitePanel extends JPanel {
        WhitePanel() {
            setOpaque(true);
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(GRID, 1));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            setup((Graphics2D) graphics);
        }
    }

    /** Левая диаграмма: конечное число энергий, уровень выбирается кликом. */
    private final class EnergyLevelsPanel extends WhitePanel {
        EnergyLevelsPanel() {
            setMinimumSize(new Dimension(310, 300));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    int n = nearestLevel(e.getY());
                    if (n >= 1) levelSpinner.setValue(n);
                }
            });
        }
        private int yForLevel(int level) {
            int plotTop = 95;
            int plotBottom = Math.max(plotTop + 1, getHeight() - 81);
            // Равномерная вертикальная расстановка для удобства выбора.
            // В подписи явно указано, что шкала схематическая.
            return plotBottom - (int) Math.round((level - 0.5) * (plotBottom - plotTop) / settings.count);
        }
        private int nearestLevel(int clickedY) {
            int best = -1, minDist = 18;
            for (int n = 1; n <= settings.count; n++) {
                int distance = Math.abs(clickedY - yForLevel(n));
                if (distance < minDist) { minDist = distance; best = n; }
            }
            return best;
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            title(g, "Разрешённые уровни энергии", 18);
            lightLabel(g, "Щёлкните по уровню, чтобы выбрать n", 18, 47);
            int left = 68, right = getWidth() - 125;
            int top = 75, bottom = getHeight() - 64;
            if (right < left + 25 || bottom < top + 25) return;
            g.setStroke(new BasicStroke(3.0f));
            g.setColor(NAVY);
            g.drawLine(left, top, left, bottom);
            g.drawLine(right, top, right, bottom);
            g.drawLine(left, bottom, right, bottom);
            g.setStroke(new BasicStroke(1.5f));
            lightLabel(g, "V → ∞", left - 18, top - 7);
            lightLabel(g, "V → ∞", right - 17, top - 7);
            for (int n = 1; n <= settings.count; n++) {
                int y = yForLevel(n);
                boolean selected = n == settings.selectedLevel;
                if (selected) {
                    g.setColor(new Color(229, 239, 255));
                    g.fillRoundRect(left + 5, y - 13, right - left - 10, 26, 10, 10);
                }
                g.setColor(selected ? BLUE : TEAL);
                g.setStroke(new BasicStroke(selected ? 3.6f : 2f));
                g.drawLine(left + 10, y, right - 10, y);
                g.setFont(new Font(Font.SANS_SERIF, selected ? Font.BOLD : Font.PLAIN, 12));
                g.setColor(NAVY);
                g.drawString("n=" + n, 17, y + 4);
                g.drawString(String.format(Locale.US, "%.4f эВ", settings.energyEv(n)), right + 10, y + 4);
            }
            lightLabel(g, "Вертикальная шкала схематическая", 16, getHeight() - 21);
        }
    }

    /** Аналитический график ψ_n(x) или ρ_n(x) с перемещаемым указателем x. */
    private final class CurvePanel extends WhitePanel {
        private final boolean probability;
        CurvePanel(boolean probability) {
            this.probability = probability;
            setMinimumSize(new Dimension(360, 210));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent event) { selectX(event.getX()); }
            });
            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override public void mouseDragged(MouseEvent event) { selectX(event.getX()); }
            });
        }
        private void selectX(int mouseX) {
            int x0 = 56, x1 = getWidth() - 21;
            if (x1 <= x0) return;
            int newValue = (int) Math.round(1000.0 * (mouseX - x0) / (x1 - x0));
            positionSlider.setValue(Math.max(0, Math.min(1000, newValue)));
        }
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            title(g, probability ? "Плотность вероятности  |ψₙ(x)|²" : "Волновая функция  ψₙ(x)", 18);
            int x0 = 56, x1 = getWidth() - 21, y0 = 56, y1 = getHeight() - 35;
            if (x1 < x0 + 30 || y1 < y0 + 25) return;
            int n = settings.selectedLevel;
            double width = settings.widthNm;
            double maxPsi = Math.sqrt(2 / width);
            double maxRho = 2 / width;
            double middle = (y0 + y1) / 2.0;
            int zeroY = probability ? y1 : (int) middle;
            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            for (int i = 0; i <= 4; i++) {
                int gx = x0 + (x1 - x0) * i / 4;
                g.drawLine(gx, y0, gx, y1);
                lightLabel(g, String.format(Locale.US, "%.2f", width * i / 4), gx - 12, getHeight() - 14);
            }
            for (int i = 0; i <= 4; i++) {
                int gy = y0 + (y1 - y0) * i / 4;
                g.drawLine(x0, gy, x1, gy);
            }
            g.setColor(SLATE);
            g.setStroke(new BasicStroke(1.4f));
            g.drawLine(x0, zeroY, x1, zeroY);
            lightLabel(g, "x, нм", x1 - 34, getHeight() - 7);
            lightLabel(g, probability ? "нм⁻¹" : "нм⁻½", x0, 47);
            Path2D path = new Path2D.Double();
            final int samples = 750;
            for (int i = 0; i <= samples; i++) {
                double fraction = i / (double) samples;
                double x = width * fraction;
                double value = probability ? PotentialWellSpectrum.probabilityDensity(n, x, width)
                        : PotentialWellSpectrum.waveFunction(n, x, width);
                double yy = probability
                        ? y1 - (value / maxRho) * (y1 - y0 - 9)
                        : middle - (value / maxPsi) * (y1 - y0) * 0.43;
                double xx = x0 + (x1 - x0) * fraction;
                if (i == 0) path.moveTo(xx, yy); else path.lineTo(xx, yy);
            }
            g.setColor(probability ? TEAL : BLUE);
            g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(path);
            double value = probability ? PotentialWellSpectrum.probabilityDensity(n, width * settings.xFraction, width)
                    : PotentialWellSpectrum.waveFunction(n, width * settings.xFraction, width);
            double py = probability
                    ? y1 - (value / maxRho) * (y1 - y0 - 9)
                    : middle - (value / maxPsi) * (y1 - y0) * 0.43;
            double px = x0 + (x1 - x0) * settings.xFraction;
            g.setColor(ORANGE);
            g.setStroke(new BasicStroke(1.25f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    1f, new float[]{5f, 5f}, 0));
            g.drawLine((int) px, y0, (int) px, y1);
            g.setStroke(new BasicStroke(1f));
            g.fillOval((int) Math.round(px) - 5, (int) Math.round(py) - 5, 10, 10);
        }
    }

    /** Вторая задача: явный переход 2 → 1 и характеристики фотона. */
    private final class TransitionPanel extends WhitePanel {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics;
            title(g, "Излучение фотона при переходе электрона 2 → 1", 20);
            lightLabel(g, "Энергии рассчитаны для массы электрона, m = mₑ", 20, 49);
            int w = getWidth(), h = getHeight();
            int diagramLeft = 73, diagramRight = Math.max(diagramLeft + 70, (int) (w * 0.58));
            int upperY = Math.max(118, (int) (h * 0.31));
            int lowerY = Math.max(upperY + 85, (int) (h * 0.73));
            if (lowerY >= h - 35) lowerY = h - 55;
            double e1 = settings.electronEnergyEv(1);
            double e2 = settings.electronEnergyEv(2);
            double delta = e2 - e1;
            double wavelength = settings.wavelengthNm();
            double frequencyHz = delta * ELECTRON_VOLT / PLANCK;
            g.setStroke(new BasicStroke(3f));
            g.setColor(NAVY);
            g.drawLine(diagramLeft, 82, diagramLeft, h - 49);
            g.drawLine(diagramRight, 82, diagramRight, h - 49);
            g.drawLine(diagramLeft, h - 49, diagramRight, h - 49);
            g.setStroke(new BasicStroke(2.8f));
            g.setColor(BLUE);
            g.drawLine(diagramLeft + 14, upperY, diagramRight - 14, upperY);
            g.setColor(TEAL);
            g.drawLine(diagramLeft + 14, lowerY, diagramRight - 14, lowerY);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.setColor(NAVY);
            g.drawString("n = 2", diagramLeft + 18, upperY - 9);
            g.drawString("n = 1", diagramLeft + 18, lowerY - 9);
            int arrowX = (diagramLeft + diagramRight) / 2;
            int a = upperY + 18, b = lowerY - 12;
            g.setColor(ORANGE);
            g.setStroke(new BasicStroke(3.5f));
            g.drawLine(arrowX, a, arrowX, b);
            g.fillPolygon(new int[]{arrowX - 8, arrowX + 8, arrowX}, new int[]{b - 12, b - 12, b + 2}, 3);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
            g.drawString("ΔE", arrowX + 9, (a + b) / 2);

            int textX = diagramRight + 28;
            if (w - textX > 165) {
                // Горизонтальная волна обозначает уносящий энергию фотон.
                Path2D photon = new Path2D.Double();
                int waveFrom = textX, waveTo = w - 36, waveY = (upperY + lowerY) / 2;
                for (int x = waveFrom; x <= waveTo; x++) {
                    double y = waveY + 12 * Math.sin((x - waveFrom) * Math.PI / 22);
                    if (x == waveFrom) photon.moveTo(x, y); else photon.lineTo(x, y);
                }
                g.setColor(ORANGE);
                g.setStroke(new BasicStroke(2.8f));
                g.draw(photon);
                g.fillPolygon(new int[]{waveTo - 11, waveTo, waveTo - 11},
                        new int[]{waveY - 9, waveY, waveY + 9}, 3);
                g.setColor(NAVY);
                g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
                g.drawString("Излучённый фотон", textX, waveY - 31);
                g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
                g.drawString(String.format(Locale.US, "λ = %.3f нм", wavelength), textX, waveY + 41);
            }
            g.setColor(NAVY);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            int metricX = diagramLeft + 19;
            int metricY = h - 21;
            g.drawString(String.format(Locale.US, "E₁ %.4f эВ  |  E₂ %.4f эВ  |  ΔE %.4f эВ  |  ν %.3e Гц",
                    e1, e2, delta, frequencyHz), metricX, metricY);
        }
    }

    /** Отдельный режим полезен для создания PNG и проверки работы графиков без дисплея. */
    private static void renderDemo(String filename) throws IOException {
        Settings example = new Settings();
        BufferedImage result = new BufferedImage(1220, 790, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            setup(g);
            g.setColor(PALE);
            g.fillRect(0, 0, 1220, 790);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23));
            g.setColor(NAVY);
            g.drawString("ННГУ · Квантовая частица в потенциальном ящике", 25, 44);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            g.setColor(SLATE);
            g.drawString("L = 0.6 нм, масса электрона, уровни n = 1…5", 25, 72);
            // Используется тот же Java2D-код, что и в интерактивных панелях.
            drawPreviewLevels(g, example, 20, 95, 370, 315);
            drawPreviewCurve(g, example, false, 402, 95, 792, 150);
            drawPreviewCurve(g, example, true, 402, 260, 792, 150);
            drawPreviewTransition(g, example, 20, 433, 1174, 324);
        } finally {
            g.dispose();
        }
        if (!ImageIO.write(result, "png", new File(filename))) throw new IOException("Ошибка кодирования PNG");
    }

    private static void previewCard(Graphics2D g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRoundRect(x, y, width, height, 14, 14);
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x, y, width, height, 14, 14);
    }

    private static void drawPreviewLevels(Graphics2D g, Settings s, int x, int y, int width, int height) {
        previewCard(g, x, y, width, height);
        g.setColor(NAVY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString("Энергетические уровни", x + 16, y + 27);
        int xl = x + 58, xr = x + 232;
        g.setColor(NAVY);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(xl, y + 53, xl, y + height - 33);
        g.drawLine(xr, y + 53, xr, y + height - 33);
        g.drawLine(xl, y + height - 33, xr, y + height - 33);
        for (int n = 1; n <= 5; n++) {
            int yy = y + height - 40 - n * 36;
            g.setColor(n == 1 ? BLUE : TEAL);
            g.setStroke(new BasicStroke(n == 1 ? 3.2f : 2.2f));
            g.drawLine(xl + 9, yy, xr - 9, yy);
            g.setColor(NAVY);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            g.drawString("n=" + n, x + 14, yy + 4);
            g.drawString(String.format(Locale.US, "%.3f эВ", s.energyEv(n)), xr + 9, yy + 4);
        }
    }

    private static void drawPreviewCurve(Graphics2D g, Settings s, boolean probability, int x, int y, int width, int height) {
        previewCard(g, x, y, width, height);
        g.setColor(NAVY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        g.drawString(probability ? "Плотность вероятности |ψ₁(x)|²" : "Волновая функция ψ₁(x)", x + 15, y + 26);
        int xl = x + 46, xr = x + width - 25, yt = y + 44, yb = y + height - 30;
        g.setColor(GRID);
        g.setStroke(new BasicStroke(1f));
        for (int i = 0; i <= 4; i++) {
            int xx = xl + i * (xr - xl) / 4;
            g.drawLine(xx, yt, xx, yb);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
            g.setColor(SLATE);
            g.drawString(String.format(Locale.US, "%.2f", s.widthNm * i / 4), xx - 9, y + height - 12);
            g.setColor(GRID);
        }
        Path2D path = new Path2D.Double();
        for (int i = 0; i <= 700; i++) {
            double f = i / 700.0;
            double xx = xl + f * (xr - xl);
            double value = probability ? PotentialWellSpectrum.probabilityDensity(1, f * s.widthNm, s.widthNm)
                    : PotentialWellSpectrum.waveFunction(1, f * s.widthNm, s.widthNm);
            double yy = probability ? yb - (value / (2 / s.widthNm)) * (yb - yt - 3)
                    : (yt + yb) / 2.0 - value / Math.sqrt(2 / s.widthNm) * (yb - yt) * .43;
            if (i == 0) path.moveTo(xx, yy); else path.lineTo(xx, yy);
        }
        g.setColor(probability ? TEAL : BLUE);
        g.setStroke(new BasicStroke(2.6f));
        g.draw(path);
        g.setColor(ORANGE);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1f, new float[]{4f, 4f}, 0));
        int xx = (int) (xl + s.xFraction * (xr - xl));
        g.drawLine(xx, yt, xx, yb);
    }

    private static void drawPreviewTransition(Graphics2D g, Settings s, int x, int y, int width, int height) {
        previewCard(g, x, y, width, height);
        g.setColor(NAVY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        g.drawString("Электрон: переход n = 2 → n = 1, испускание фотона", x + 18, y + 30);
        int xl = x + 60, xr = x + 660, y2 = y + 96, y1 = y + 221;
        g.setColor(NAVY);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(xl, y + 52, xl, y + 268);
        g.drawLine(xr, y + 52, xr, y + 268);
        g.drawLine(xl, y + 268, xr, y + 268);
        g.setColor(BLUE);
        g.drawLine(xl + 20, y2, xr - 20, y2);
        g.setColor(TEAL);
        g.drawLine(xl + 20, y1, xr - 20, y1);
        int ax = (xl + xr) / 2;
        g.setColor(ORANGE);
        g.drawLine(ax, y2 + 15, ax, y1 - 18);
        g.fillPolygon(new int[]{ax - 8, ax + 8, ax}, new int[]{y1 - 26, y1 - 26, y1 - 10}, 3);
        g.setColor(NAVY);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        g.drawString("n = 2  •  " + String.format(Locale.US, "%.3f эВ", s.electronEnergyEv(2)), xl + 23, y2 - 12);
        g.drawString("n = 1  •  " + String.format(Locale.US, "%.3f эВ", s.electronEnergyEv(1)), xl + 23, y1 - 12);
        double delta = s.electronEnergyEv(2) - s.electronEnergyEv(1);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        g.drawString(String.format(Locale.US, "ΔE = %.3f эВ", delta), xr + 30, y + 126);
        g.drawString(String.format(Locale.US, "λ = %.3f нм", s.wavelengthNm()), xr + 30, y + 161);
        g.drawString("Излучённый фотон", xr + 30, y + 199);
        Path2D wave = new Path2D.Double();
        for (int xx = xr + 30; xx <= x + width - 35; xx++) {
            double yy = y + 225 + 12 * Math.sin((xx - xr - 30) * Math.PI / 20);
            if (xx == xr + 30) wave.moveTo(xx, yy); else wave.lineTo(xx, yy);
        }
        g.setColor(ORANGE);
        g.setStroke(new BasicStroke(2.7f));
        g.draw(wave);
    }

    public static void main(String[] args) {
        if (args.length == 2 && "--demo-png".equals(args[0])) {
            try {
                renderDemo(args[1]);
                System.out.println("PNG создан: " + new File(args[1]).getAbsolutePath());
            } catch (IOException ex) {
                System.err.println("Не удалось создать PNG: " + ex.getMessage());
                System.exit(1);
            }
            return;
        }
        if (args.length > 0) {
            System.err.println("Использование: java PotentialWellVisualizer [--demo-png путь.png]");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Стандартное оформление Swing подходит для любой ОС.
            }
            new PotentialWellVisualizer().frame.setVisible(true);
        });
    }
}

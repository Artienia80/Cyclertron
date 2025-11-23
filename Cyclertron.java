import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Cyclertron - High-Fidelity Orbital Mechanics Simulator using Orekit
 * Uses real NASA JPL Keplerian elements for accurate planetary positions
 */
public class Cyclertron extends JFrame {
    private static final int WIDTH = 1600;
    private static final int HEIGHT = 1000;
    private OrebitSimulationPanel simulationPanel;

    public Cyclertron() {
        setTitle("Cyclertron - Orekit Orbital Mechanics Simulator");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize Orekit with local data directory
        try {
            System.out.println("Initializing Orekit...");
            DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();

            File orekitData = new File("orekit-data");
            if (orekitData.exists()) {
                manager.addProvider(new DirectoryCrawler(orekitData));
                System.out.println("Using local orekit-data directory");
            } else {
                File defaultData = new File(System.getProperty("user.home"), "orekit-data");
                if (defaultData.exists()) {
                    manager.addProvider(new DirectoryCrawler(defaultData));
                    System.out.println("Using orekit-data from user home directory");
                } else {
                    System.out.println("Attempting to use orekit-data from classpath...");
                    manager.addProvider(new DirectoryCrawler(new File("src/main/resources/orekit-data")));
                }
            }

            System.out.println("Orekit initialized successfully!");

            simulationPanel = new OrebitSimulationPanel();
            add(simulationPanel);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error initializing Orekit: " + e.getMessage() +
                            "\n\nPlease download orekit-data from:" +
                            "\nhttps://gitlab.orekit.org/orekit/orekit-data" +
                            "\nand extract it to: orekit-data/ or ~/orekit-data/",
                    "Initialization Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Cyclertron cyclertron = new Cyclertron();
            cyclertron.setVisible(true);
        });
    }
}

/**
 * Panel containing the Orekit simulation with real physics
 */
class OrebitSimulationPanel extends JPanel {
    private OrebitEngine engine;
    private Timer renderTimer;
    private JButton playPauseButton;
    private JButton stopButton;
    private JButton speedUpButton;
    private JButton slowDownButton;
    private JButton reverseButton;
    private JButton resetButton;
    private JButton setNowButton;
    private JButton jumpToDateButton;
    private JSpinner speedSpinner;
    private JComboBox<TimeUnit> timeUnitCombo;
    private JLabel infoLabel;
    private JLabel dateLabel;
    private boolean isPaused = false;
    private List<List<Point>> trajectories;
    private static final int MAX_TRAJECTORY_POINTS = 1000;

    private DetailPanel detailPanel;

    private static final double SCALE = 120.0; // AU to pixels

    enum TimeUnit {
        SECONDS("Seconds", 1.0 / 86400.0),
        MINUTES("Minutes", 1.0 / 1440.0),
        HOURS("Hours", 1.0 / 24.0),
        DAYS("Days", 1.0),
        WEEKS("Weeks", 7.0),
        MONTHS("Months", 30.0),
        YEARS("Years", 365.25);

        final String label;
        final double daysPerUnit;

        TimeUnit(String label, double daysPerUnit) {
            this.label = label;
            this.daysPerUnit = daysPerUnit;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public OrebitSimulationPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // Initialize physics engine
        engine = new OrebitEngine();

        // Initialize trajectory tracking
        trajectories = new ArrayList<>();
        for (int i = 0; i < engine.getPlanets().size(); i++) {
            trajectories.add(new ArrayList<>());
        }

        // Create split panel for main view and detail view
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(1100);

        // Main simulation panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintMainView(g);
            }
        };
        mainPanel.setBackground(Color.BLACK);

        // Detail panel
        detailPanel = new DetailPanel(engine);

        splitPane.setLeftComponent(mainPanel);
        splitPane.setRightComponent(detailPanel);

        // Create main control panel
        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new BoxLayout(mainControlPanel, BoxLayout.Y_AXIS));
        mainControlPanel.setBackground(new Color(30, 30, 30));

        // Playback controls row
        JPanel playbackPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        playbackPanel.setBackground(new Color(30, 30, 30));

        playPauseButton = new JButton("Pause");
        playPauseButton.addActionListener(e -> togglePause());

        stopButton = new JButton("Stop");
        stopButton.addActionListener(e -> {
            engine.stop();
            isPaused = true;
            playPauseButton.setText("Play");
            updateLabels();
        });

        speedUpButton = new JButton("Speed Up (2x)");
        speedUpButton.addActionListener(e -> {
            engine.increaseSpeed();
            updateSpeedSpinner();
            updateLabels();
        });

        slowDownButton = new JButton("Slow Down (0.5x)");
        slowDownButton.addActionListener(e -> {
            engine.decreaseSpeed();
            updateSpeedSpinner();
            updateLabels();
        });

        reverseButton = new JButton("Reverse");
        reverseButton.addActionListener(e -> {
            engine.reverseSpeed();
            updateSpeedSpinner();
            updateLabels();
        });

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> {
            engine.reset();
            clearTrajectories();
            isPaused = false;
            playPauseButton.setText("Pause");
            updateSpeedSpinner();
            updateLabels();
        });

        playbackPanel.add(playPauseButton);
        playbackPanel.add(stopButton);
        playbackPanel.add(speedUpButton);
        playbackPanel.add(slowDownButton);
        playbackPanel.add(reverseButton);
        playbackPanel.add(resetButton);

        // Speed control row
        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        speedPanel.setBackground(new Color(30, 30, 30));

        JLabel speedLabel = new JLabel("Speed:");
        speedLabel.setForeground(Color.WHITE);

        SpinnerNumberModel speedModel = new SpinnerNumberModel(1.0, -1000.0, 1000.0, 0.1);
        speedSpinner = new JSpinner(speedModel);
        speedSpinner.setPreferredSize(new Dimension(80, 25));
        speedSpinner.addChangeListener(e -> {
            double value = (Double) speedSpinner.getValue();
            TimeUnit unit = (TimeUnit) timeUnitCombo.getSelectedItem();
            engine.setTimeSpeed(value * unit.daysPerUnit);
            updateLabels();
        });

        timeUnitCombo = new JComboBox<>(TimeUnit.values());
        timeUnitCombo.setSelectedItem(TimeUnit.DAYS);
        timeUnitCombo.addActionListener(e -> {
            double value = (Double) speedSpinner.getValue();
            TimeUnit unit = (TimeUnit) timeUnitCombo.getSelectedItem();
            engine.setTimeSpeed(value * unit.daysPerUnit);
            updateLabels();
        });

        JLabel perLabel = new JLabel("per frame");
        perLabel.setForeground(Color.WHITE);

        speedPanel.add(speedLabel);
        speedPanel.add(speedSpinner);
        speedPanel.add(timeUnitCombo);
        speedPanel.add(perLabel);

        // Date control row
        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        datePanel.setBackground(new Color(30, 30, 30));

        setNowButton = new JButton("Set to Now");
        setNowButton.addActionListener(e -> {
            engine.setToCurrentDate();
            clearTrajectories();
            updateLabels();
        });

        jumpToDateButton = new JButton("Jump to Date...");
        jumpToDateButton.addActionListener(e -> showDatePicker());

        dateLabel = new JLabel();
        dateLabel.setForeground(Color.WHITE);
        dateLabel.setFont(new Font("Monospace", Font.BOLD, 12));

        datePanel.add(setNowButton);
        datePanel.add(jumpToDateButton);
        datePanel.add(dateLabel);

        // Info label row
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        infoPanel.setBackground(new Color(30, 30, 30));

        infoLabel = new JLabel();
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Monospace", Font.PLAIN, 11));

        infoPanel.add(infoLabel);

        // Add all panels
        mainControlPanel.add(playbackPanel);
        mainControlPanel.add(speedPanel);
        mainControlPanel.add(datePanel);
        mainControlPanel.add(infoPanel);

        add(splitPane, BorderLayout.CENTER);
        add(mainControlPanel, BorderLayout.SOUTH);

        updateLabels();

        // Render timer (60 FPS)
        renderTimer = new Timer(16, e -> {
            if (!isPaused) {
                engine.update();
                updateTrajectories();
                updateLabels();
            }
            mainPanel.repaint();
            detailPanel.repaint();
        });
        renderTimer.start();
    }

    private void togglePause() {
        isPaused = !isPaused;
        playPauseButton.setText(isPaused ? "Play" : "Pause");
    }

    private void updateSpeedSpinner() {
        TimeUnit unit = (TimeUnit) timeUnitCombo.getSelectedItem();
        double speedInDays = engine.getTimeSpeed();
        double speedInUnit = speedInDays / unit.daysPerUnit;
        speedSpinner.setValue(speedInUnit);
    }

    private void updateTrajectories() {
        int centerX = 550;
        int centerY = 400;

        List<PlanetState> planets = engine.getPlanets();
        for (int i = 0; i < planets.size(); i++) {
            Vector3D pos = planets.get(i).getPosition();
            int x = (int)(centerX + pos.getX() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            List<Point> trajectory = trajectories.get(i);
            trajectory.add(new Point(x, y));

            if (trajectory.size() > MAX_TRAJECTORY_POINTS) {
                trajectory.remove(0);
            }
        }
    }

    private void clearTrajectories() {
        for (List<Point> trajectory : trajectories) {
            trajectory.clear();
        }
    }

    private void updateLabels() {
        infoLabel.setText(String.format("Speed: %.2f days/frame | Elapsed: %.1f days",
                engine.getTimeSpeed(), engine.getElapsedDays()));

        AbsoluteDate currentDate = engine.getCurrentDate();
        dateLabel.setText(String.format("Date: %04d-%02d-%02d %02d:%02d:%02d",
                currentDate.getComponents(TimeScalesFactory.getUTC()).getDate().getYear(),
                currentDate.getComponents(TimeScalesFactory.getUTC()).getDate().getMonth(),
                currentDate.getComponents(TimeScalesFactory.getUTC()).getDate().getDay(),
                currentDate.getComponents(TimeScalesFactory.getUTC()).getTime().getHour(),
                currentDate.getComponents(TimeScalesFactory.getUTC()).getTime().getMinute(),
                (int) currentDate.getComponents(TimeScalesFactory.getUTC()).getTime().getSecond()));
    }

    private void showDatePicker() {
        JDialog dialog = new JDialog((java.awt.Frame) SwingUtilities.getWindowAncestor(this), "Select Date", true);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel centerPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel yearLabel = new JLabel("Year:");
        SpinnerNumberModel yearModel = new SpinnerNumberModel(2000, 1950, 2100, 1);
        JSpinner yearSpinner = new JSpinner(yearModel);

        JLabel monthLabel = new JLabel("Month:");
        SpinnerNumberModel monthModel = new SpinnerNumberModel(1, 1, 12, 1);
        JSpinner monthSpinner = new JSpinner(monthModel);

        JLabel dayLabel = new JLabel("Day:");
        SpinnerNumberModel dayModel = new SpinnerNumberModel(1, 1, 31, 1);
        JSpinner daySpinner = new JSpinner(dayModel);

        centerPanel.add(yearLabel);
        centerPanel.add(yearSpinner);
        centerPanel.add(monthLabel);
        centerPanel.add(monthSpinner);
        centerPanel.add(dayLabel);
        centerPanel.add(daySpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JButton okButton = new JButton("Jump to Date");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(e -> {
            int year = (Integer) yearSpinner.getValue();
            int month = (Integer) monthSpinner.getValue();
            int day = (Integer) daySpinner.getValue();

            try {
                engine.jumpToDate(year, month, day);
                clearTrajectories();
                updateLabels();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid date: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void paintMainView(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = 550;
        int centerY = 400;

        // Draw accurate orbit ellipses
        g2d.setStroke(new BasicStroke(1.5f));
        List<PlanetState> planets = engine.getPlanets();
        for (int i = 0; i < planets.size(); i++) {
            PlanetState planet = planets.get(i);
            g2d.setColor(new Color(planet.getColor().getRed(),
                    planet.getColor().getGreen(),
                    planet.getColor().getBlue(), 60));

            drawOrbitPath(g2d, planet, centerX, centerY);
        }

        // Draw trajectories
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < trajectories.size(); i++) {
            List<Point> trajectory = trajectories.get(i);
            if (trajectory.size() > 1) {
                PlanetState planet = planets.get(i);
                g2d.setColor(new Color(planet.getColor().getRed(),
                        planet.getColor().getGreen(),
                        planet.getColor().getBlue(), 180));

                for (int j = 0; j < trajectory.size() - 1; j++) {
                    Point p1 = trajectory.get(j);
                    Point p2 = trajectory.get(j + 1);
                    g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
                }
            }
        }

        // Draw Sun
        g2d.setColor(Color.YELLOW);
        int sunRadius = 25;
        for (int i = 2; i >= 0; i--) {
            int glowRadius = sunRadius + i * 8;
            g2d.setColor(new Color(255, 255, 0, 40 - i * 12));
            g2d.fillOval(centerX - glowRadius / 2, centerY - glowRadius / 2,
                    glowRadius, glowRadius);
        }
        g2d.setColor(Color.YELLOW);
        g2d.fillOval(centerX - sunRadius / 2, centerY - sunRadius / 2, sunRadius, sunRadius);

        // Draw planets
        for (PlanetState planet : planets) {
            Vector3D pos = planet.getPosition();
            int x = (int)(centerX + pos.getX() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            g2d.setColor(planet.getColor());
            int radius = planet.getRadius();
            g2d.fillOval(x - radius / 2, y - radius / 2, radius, radius);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.drawString(planet.getName(), x + radius, y);
        }

        // Info text
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Monospace", Font.PLAIN, 12));
        int y = 25;
        g2d.drawString("=== OREKIT PHYSICS SIMULATION ===", 15, y);
        y += 20;
        g2d.drawString("Using NASA JPL Keplerian Elements", 15, y);
        y += 25;
        for (PlanetState planet : planets) {
            Vector3D pos = planet.getPosition();
            double distanceAU = pos.getNorm() / Constants.IAU_2012_ASTRONOMICAL_UNIT;
            Vector3D vel = planet.getVelocity();
            double speedKms = vel.getNorm() / 1000.0;
            g2d.drawString(String.format("%s: %.4f AU | %.2f km/s",
                    planet.getName(), distanceAU, speedKms), 15, y);
            y += 18;
        }
    }

    private void drawOrbitPath(Graphics2D g2d, PlanetState planet, int centerX, int centerY) {
        try {
            int numPoints = 360;
            int[] xPoints = new int[numPoints];
            int[] yPoints = new int[numPoints];

            AbsoluteDate startDate = engine.getCurrentDate();
            double orbitalPeriod = planet.getOrbitalPeriod();

            for (int i = 0; i < numPoints; i++) {
                double fraction = (double) i / numPoints;
                AbsoluteDate sampleDate = startDate.shiftedBy(fraction * orbitalPeriod);
                Vector3D pos = planet.getPositionAtDate(sampleDate);

                xPoints[i] = (int)(centerX + pos.getX() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);
                yPoints[i] = (int)(centerY + pos.getY() * SCALE / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            }

            g2d.drawPolygon(xPoints, yPoints, numPoints);
        } catch (Exception e) {
            int orbitRadius = (int)(planet.getSemiMajorAxis() * SCALE);
            g2d.drawOval(centerX - orbitRadius, centerY - orbitRadius,
                    orbitRadius * 2, orbitRadius * 2);
        }
    }
}

/**
 * Detail panel showing Earth-Moon and Mars-Phobos-Deimos systems
 */
class DetailPanel extends JPanel {
    private OrebitEngine engine;

    public DetailPanel(OrebitEngine engine) {
        this.engine = engine;
        setBackground(Color.BLACK);
        setLayout(new GridLayout(2, 1, 5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelHeight = getHeight() / 2;

        // Draw Earth-Moon system
        drawEarthMoonSystem(g2d, getWidth() / 2, panelHeight / 2);

        // Draw Mars-Phobos-Deimos system
        drawMarsSystem(g2d, getWidth() / 2, panelHeight + panelHeight / 2);
    }

    private void drawEarthMoonSystem(Graphics2D g2d, int centerX, int centerY) {
        double scale = 0.8; // km to pixels

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Earth-Moon System", centerX - 70, 20);

        PlanetState earth = engine.getPlanetByName("Earth");
        if (earth == null) return;

        MoonState moon = earth.getMoon();
        if (moon == null) return;

        // Draw Moon's orbit
        g2d.setColor(new Color(200, 200, 200, 80));
        g2d.setStroke(new BasicStroke(1));
        int moonOrbitRadius = (int)(moon.getSemiMajorAxis() / 1000.0 * scale);
        g2d.drawOval(centerX - moonOrbitRadius, centerY - moonOrbitRadius,
                moonOrbitRadius * 2, moonOrbitRadius * 2);

        // Draw Earth
        g2d.setColor(new Color(100, 149, 237));
        int earthRadius = 25;
        g2d.fillOval(centerX - earthRadius / 2, centerY - earthRadius / 2, earthRadius, earthRadius);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Earth", centerX - 15, centerY + earthRadius);

        // Draw Moon
        Vector3D moonPos = moon.getRelativePosition();
        int moonX = (int)(centerX + moonPos.getX() / 1000.0 * scale);
        int moonY = (int)(centerY + moonPos.getY() / 1000.0 * scale);

        g2d.setColor(new Color(200, 200, 200));
        int moonRadius = 8;
        g2d.fillOval(moonX - moonRadius / 2, moonY - moonRadius / 2, moonRadius, moonRadius);
        g2d.setColor(Color.WHITE);
        g2d.drawString("Moon", moonX + moonRadius, moonY);

        // Info
        g2d.setFont(new Font("Monospace", Font.PLAIN, 10));
        int infoY = centerY + 120;
        g2d.drawString(String.format("Moon Distance: %.0f km", moonPos.getNorm() / 1000.0), 10, infoY);
        infoY += 15;
        g2d.drawString(String.format("Orbital Period: 27.3 days"), 10, infoY);
    }

    private void drawMarsSystem(Graphics2D g2d, int centerX, int centerY) {
        double scale = 6.0; // km to pixels

        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Mars-Phobos-Deimos System", centerX - 100, centerY - 180);

        PlanetState mars = engine.getPlanetByName("Mars");
        if (mars == null) return;

        List<MoonState> moons = mars.getMoons();

        // Draw moon orbits
        g2d.setStroke(new BasicStroke(1));
        for (MoonState moon : moons) {
            g2d.setColor(new Color(moon.getColor().getRed(),
                    moon.getColor().getGreen(),
                    moon.getColor().getBlue(), 80));
            int orbitRadius = (int)(moon.getSemiMajorAxis() / 1000.0 * scale);
            g2d.drawOval(centerX - orbitRadius, centerY - orbitRadius,
                    orbitRadius * 2, orbitRadius * 2);
        }

        // Draw Mars
        g2d.setColor(new Color(205, 92, 92));
        int marsRadius = 20;
        g2d.fillOval(centerX - marsRadius / 2, centerY - marsRadius / 2, marsRadius, marsRadius);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Mars", centerX - 12, centerY + marsRadius);

        // Draw moons
        for (MoonState moon : moons) {
            Vector3D moonPos = moon.getRelativePosition();
            int moonX = (int)(centerX + moonPos.getX() / 1000.0 * scale);
            int moonY = (int)(centerY + moonPos.getY() / 1000.0 * scale);

            g2d.setColor(moon.getColor());
            int moonRadius = moon.getDisplayRadius();
            g2d.fillOval(moonX - moonRadius / 2, moonY - moonRadius / 2, moonRadius, moonRadius);
            g2d.setColor(Color.WHITE);
            g2d.drawString(moon.getName(), moonX + moonRadius, moonY);
        }

        // Info
        g2d.setFont(new Font("Monospace", Font.PLAIN, 10));
        int infoY = centerY + 120;
        for (MoonState moon : moons) {
            Vector3D moonPos = moon.getRelativePosition();
            g2d.drawString(String.format("%s: %.0f km | Period: %.2f hrs",
                    moon.getName(),
                    moonPos.getNorm() / 1000.0,
                    moon.getOrbitalPeriod() / 3600.0), 10, infoY);
            infoY += 15;
        }
    }
}

/**
 * Physics engine using Orekit
 */
class OrebitEngine {
    private Frame sunCentricFrame;
    private AbsoluteDate initialDate;
    private AbsoluteDate currentDate;
    private List<PlanetState> planets;
    private double timeSpeed = 1.0;
    private double elapsedDays = 0;

    // NASA JPL Keplerian Elements (J2000)
    private static final double[][] ELEMENTS = {
            {1.00000261, 0.01671123, -0.00001531, 100.46457166, 102.93768193, 0.0}, // Earth
            {1.52371034, 0.09339410, 1.84969142, -4.55343205, -23.94362959, 49.55953891}  // Mars
    };

    public OrebitEngine() {
        try {
            System.out.println("Initializing orbital mechanics engine...");
            sunCentricFrame = FramesFactory.getEME2000();
            initialDate = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0, TimeScalesFactory.getTT());
            currentDate = initialDate;

            planets = new ArrayList<>();
            planets.add(createEarth());
            planets.add(createMars());

            System.out.println("Engine initialized successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to initialize engine: " + e.getMessage());
        }
    }

    private PlanetState createEarth() {
        try {
            double[] elements = ELEMENTS[0];
            double a = elements[0] * Constants.IAU_2012_ASTRONOMICAL_UNIT;
            double e = elements[1];
            double i = Math.toRadians(elements[2]);
            double L = Math.toRadians(elements[3]);
            double longPeri = Math.toRadians(elements[4]);
            double longNode = Math.toRadians(elements[5]);

            double omega = longPeri - longNode;
            double M = L - longPeri;

            Orbit orbit = new KeplerianOrbit(
                    a, e, i, omega, longNode, M,
                    PositionAngleType.MEAN,
                    sunCentricFrame,
                    initialDate,
                    Constants.IAU_2015_NOMINAL_SUN_GM
            );

            KeplerianPropagator propagator = new KeplerianPropagator(orbit);
            PlanetState earth = new PlanetState("Earth", propagator, new Color(100, 149, 237), 12, a);

            // Add Moon
            // Moon orbital data: semi-major axis 384,400 km, eccentricity 0.0549, inclination 5.145°
            Frame earthFrame = FramesFactory.getEME2000(); // Approximation
            double moonA = 384400000.0; // meters
            double moonE = 0.0549;
            double moonI = Math.toRadians(5.145);
            double moonOmega = 0.0;
            double moonNode = 0.0;
            double moonM = 0.0;

            Orbit moonOrbit = new KeplerianOrbit(
                    moonA, moonE, moonI, moonOmega, moonNode, moonM,
                    PositionAngleType.MEAN,
                    earthFrame,
                    initialDate,
                    3.986004418e14  // Earth's gravitational parameter
            );

            KeplerianPropagator moonPropagator = new KeplerianPropagator(moonOrbit);
            MoonState moon = new MoonState("Moon", moonPropagator, new Color(200, 200, 200), 8, moonA);
            earth.setMoon(moon);

            System.out.println("Created Earth with Moon");
            return earth;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create Earth: " + e.getMessage());
            return null;
        }
    }

    private PlanetState createMars() {
        try {
            double[] elements = ELEMENTS[1];
            double a = elements[0] * Constants.IAU_2012_ASTRONOMICAL_UNIT;
            double e = elements[1];
            double i = Math.toRadians(elements[2]);
            double L = Math.toRadians(elements[3]);
            double longPeri = Math.toRadians(elements[4]);
            double longNode = Math.toRadians(elements[5]);

            double omega = longPeri - longNode;
            double M = L - longPeri;

            Orbit orbit = new KeplerianOrbit(
                    a, e, i, omega, longNode, M,
                    PositionAngleType.MEAN,
                    sunCentricFrame,
                    initialDate,
                    Constants.IAU_2015_NOMINAL_SUN_GM
            );

            KeplerianPropagator propagator = new KeplerianPropagator(orbit);
            PlanetState mars = new PlanetState("Mars", propagator, new Color(205, 92, 92), 10, a);

            // Add Phobos and Deimos
            Frame marsFrame = FramesFactory.getEME2000(); // Approximation

            // Phobos: semi-major axis 9,377 km, eccentricity 0.0151, orbital period 7.66 hours
            double phobosA = 9377000.0; // meters
            double phobosE = 0.0151;
            double phobosI = Math.toRadians(1.075);

            Orbit phobosOrbit = new KeplerianOrbit(
                    phobosA, phobosE, phobosI, 0.0, 0.0, 0.0,
                    PositionAngleType.MEAN,
                    marsFrame,
                    initialDate,
                    4.282837e13  // Mars's gravitational parameter
            );

            KeplerianPropagator phobosPropagator = new KeplerianPropagator(phobosOrbit);
            MoonState phobos = new MoonState("Phobos", phobosPropagator, new Color(180, 140, 100), 6, phobosA);

            // Deimos: semi-major axis 23,460 km, eccentricity 0.0002, orbital period 30.35 hours
            double deimosA = 23460000.0; // meters
            double deimosE = 0.0002;
            double deimosI = Math.toRadians(1.788);

            Orbit deimosOrbit = new KeplerianOrbit(
                    deimosA, deimosE, deimosI, 0.0, 0.0, Math.PI,
                    PositionAngleType.MEAN,
                    marsFrame,
                    initialDate,
                    4.282837e13  // Mars's gravitational parameter
            );

            KeplerianPropagator deimosPropagator = new KeplerianPropagator(deimosOrbit);
            MoonState deimos = new MoonState("Deimos", deimosPropagator, new Color(160, 120, 80), 5, deimosA);

            mars.addMoon(phobos);
            mars.addMoon(deimos);

            System.out.println("Created Mars with Phobos and Deimos");
            return mars;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create Mars: " + e.getMessage());
            return null;
        }
    }

    public void update() {
        currentDate = currentDate.shiftedBy(timeSpeed * 86400.0);
        elapsedDays += timeSpeed;

        for (PlanetState planet : planets) {
            planet.update(currentDate);
        }
    }

    public void stop() {
        timeSpeed = 0.0;
    }

    public void reset() {
        currentDate = initialDate;
        elapsedDays = 0;
        timeSpeed = 1.0;
        for (PlanetState planet : planets) {
            planet.update(currentDate);
        }
    }

    public void increaseSpeed() {
        if (timeSpeed == 0) {
            timeSpeed = 0.1;
        } else {
            timeSpeed *= 2.0;
        }
        if (timeSpeed > 100) timeSpeed = 100;
    }

    public void decreaseSpeed() {
        timeSpeed *= 0.5;
        if (Math.abs(timeSpeed) < 0.1) timeSpeed = Math.signum(timeSpeed) * 0.1;
    }

    public void reverseSpeed() {
        timeSpeed = -timeSpeed;
    }

    public void setTimeSpeed(double speed) {
        this.timeSpeed = speed;
    }

    public void setToCurrentDate() {
        try {
            LocalDateTime now = LocalDateTime.now();
            currentDate = new AbsoluteDate(
                    now.getYear(),
                    now.getMonthValue(),
                    now.getDayOfMonth(),
                    now.getHour(),
                    now.getMinute(),
                    now.getSecond(),
                    TimeScalesFactory.getUTC()
            );
            elapsedDays = currentDate.durationFrom(initialDate) / 86400.0;

            for (PlanetState planet : planets) {
                planet.update(currentDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void jumpToDate(int year, int month, int day) {
        try {
            currentDate = new AbsoluteDate(year, month, day, 12, 0, 0.0, TimeScalesFactory.getUTC());
            elapsedDays = currentDate.durationFrom(initialDate) / 86400.0;

            for (PlanetState planet : planets) {
                planet.update(currentDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid date", e);
        }
    }

    public List<PlanetState> getPlanets() { return planets; }
    public double getTimeSpeed() { return timeSpeed; }
    public double getElapsedDays() { return elapsedDays; }
    public AbsoluteDate getCurrentDate() { return currentDate; }

    public PlanetState getPlanetByName(String name) {
        for (PlanetState planet : planets) {
            if (planet.getName().equals(name)) {
                return planet;
            }
        }
        return null;
    }
}

/**
 * Represents a planet's state using Orekit propagation
 */
class PlanetState {
    private String name;
    private KeplerianPropagator propagator;
    private Vector3D position;
    private Vector3D velocity;
    private Color color;
    private int radius;
    private double semiMajorAxis;
    private MoonState moon;
    private List<MoonState> moons;

    public PlanetState(String name, KeplerianPropagator propagator,
                       Color color, int radius, double semiMajorAxis) {
        this.name = name;
        this.propagator = propagator;
        this.color = color;
        this.radius = radius;
        this.semiMajorAxis = semiMajorAxis;
        this.position = new Vector3D(0, 0, 0);
        this.velocity = new Vector3D(0, 0, 0);
        this.moons = new ArrayList<>();
    }

    public void update(AbsoluteDate date) {
        try {
            var state = propagator.propagate(date);
            position = state.getPosition();
            velocity = state.getPVCoordinates().getVelocity();

            // Update moon
            if (moon != null) {
                moon.update(date);
            }

            // Update moons
            for (MoonState m : moons) {
                m.update(date);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Vector3D getPositionAtDate(AbsoluteDate date) {
        try {
            var state = propagator.propagate(date);
            return state.getPosition();
        } catch (Exception e) {
            e.printStackTrace();
            return new Vector3D(0, 0, 0);
        }
    }

    public double getOrbitalPeriod() {
        try {
            double a = semiMajorAxis;
            double mu = Constants.IAU_2015_NOMINAL_SUN_GM;
            return 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
        } catch (Exception e) {
            e.printStackTrace();
            return 365.25 * 86400.0;
        }
    }

    public String getName() { return name; }
    public Vector3D getPosition() { return position; }
    public Vector3D getVelocity() { return velocity; }
    public Color getColor() { return color; }
    public int getRadius() { return radius; }
    public double getSemiMajorAxis() { return semiMajorAxis / Constants.IAU_2012_ASTRONOMICAL_UNIT; }

    public void setMoon(MoonState moon) { this.moon = moon; }
    public MoonState getMoon() { return moon; }

    public void addMoon(MoonState moon) { this.moons.add(moon); }
    public List<MoonState> getMoons() { return moons; }
}

/**
 * Represents a moon orbiting a planet
 */
class MoonState {
    private String name;
    private KeplerianPropagator propagator;
    private Vector3D relativePosition;
    private Color color;
    private int displayRadius;
    private double semiMajorAxis;

    public MoonState(String name, KeplerianPropagator propagator,
                     Color color, int displayRadius, double semiMajorAxis) {
        this.name = name;
        this.propagator = propagator;
        this.color = color;
        this.displayRadius = displayRadius;
        this.semiMajorAxis = semiMajorAxis;
        this.relativePosition = new Vector3D(0, 0, 0);
    }

    public void update(AbsoluteDate date) {
        try {
            var state = propagator.propagate(date);
            relativePosition = state.getPosition();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public double getOrbitalPeriod() {
        try {
            double a = semiMajorAxis;
            double mu;
            if (name.equals("Moon")) {
                mu = 3.986004418e14; // Earth's GM
            } else {
                mu = 4.282837e13; // Mars's GM
            }
            return 2 * Math.PI * Math.sqrt(Math.pow(a, 3) / mu);
        } catch (Exception e) {
            e.printStackTrace();
            return 86400.0;
        }
    }

    public String getName() { return name; }
    public Vector3D getRelativePosition() { return relativePosition; }
    public Color getColor() { return color; }
    public int getDisplayRadius() { return displayRadius; }
    public double getSemiMajorAxis() { return semiMajorAxis; }
}
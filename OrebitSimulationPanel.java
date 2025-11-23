import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Main simulation panel with controls and split view
 */
public class OrebitSimulationPanel extends JPanel {
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
    private JButton mainZoomInButton;
    private JButton mainZoomOutButton;
    private JSpinner speedSpinner;
    private JComboBox<TimeUnit> timeUnitCombo;
    private JLabel infoLabel;
    private JLabel dateLabel;
    private boolean isPaused = false;
    private List<List<Point>> trajectories;
    private List<List<Point>> spacecraftTrajectories;
    private static final int MAX_TRAJECTORY_POINTS = 1000;

    private DetailPanel detailPanel;
    private JPanel mainPanel;

    private double mainZoom = 120.0; // AU to pixels - adjustable zoom
    private List<SpacecraftState> spacecraft;

    public OrebitSimulationPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        // Initialize physics engine
        engine = new OrebitEngine();

        // Get spacecraft from engine
        spacecraft = engine.getSpacecraft();

        // Initialize trajectory tracking
        trajectories = new ArrayList<>();
        for (int i = 0; i < engine.getPlanets().size(); i++) {
            trajectories.add(new ArrayList<>());
        }

        spacecraftTrajectories = new ArrayList<>();

        // Create split panel for main view and detail view
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setDividerLocation(1100);

        // Main simulation panel
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintMainView(g);
            }
        };
        mainPanel.setBackground(Color.BLACK);

        // Add mouse listener for right-click spawning
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    spawnSpacecraft(e.getX(), e.getY());
                }
            }
        });

        // Detail panel
        detailPanel = new DetailPanel(engine);

        splitPane.setLeftComponent(mainPanel);
        splitPane.setRightComponent(detailPanel);

        // Create control panel
        JPanel controlPanel = createControlPanel();

        add(splitPane, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        updateLabels();

        // Render timer (60 FPS)
        renderTimer = new Timer(16, e -> {
            if (!isPaused) {
                engine.update();
                updateTrajectories();
                updateSpacecraftTrajectories();
                updateLabels();
            }
            mainPanel.repaint();
            detailPanel.repaint();
        });
        renderTimer.start();
    }

    private JPanel createControlPanel() {
        JPanel mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new BoxLayout(mainControlPanel, BoxLayout.Y_AXIS));
        mainControlPanel.setBackground(new Color(30, 30, 30));

        // Playback controls
        mainControlPanel.add(createPlaybackPanel());

        // Speed controls
        mainControlPanel.add(createSpeedPanel());

        // Date controls
        mainControlPanel.add(createDatePanel());

        // Zoom controls
        mainControlPanel.add(createZoomPanel());

        // Info panel
        mainControlPanel.add(createInfoPanel());

        return mainControlPanel;
    }

    private JPanel createPlaybackPanel() {
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

        return playbackPanel;
    }

    private JPanel createSpeedPanel() {
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

        return speedPanel;
    }

    private JPanel createDatePanel() {
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

        return datePanel;
    }

    private JPanel createZoomPanel() {
        JPanel zoomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        zoomPanel.setBackground(new Color(30, 30, 30));

        JLabel zoomLabel = new JLabel("Main View Zoom:");
        zoomLabel.setForeground(Color.WHITE);

        mainZoomInButton = new JButton("Zoom In (+)");
        mainZoomInButton.addActionListener(e -> {
            mainZoom *= 1.5;
            if (mainZoom > 1000) mainZoom = 1000;
            mainPanel.repaint();
        });

        mainZoomOutButton = new JButton("Zoom Out (-)");
        mainZoomOutButton.addActionListener(e -> {
            mainZoom /= 1.5;
            if (mainZoom < 10) mainZoom = 10;
            mainPanel.repaint();
        });

        JButton mainZoomResetButton = new JButton("Reset Zoom");
        mainZoomResetButton.addActionListener(e -> {
            mainZoom = 120.0;
            mainPanel.repaint();
        });

        JLabel detailZoomLabel = new JLabel("  |  Detail Panels:");
        detailZoomLabel.setForeground(Color.WHITE);

        JButton detailZoomInButton = new JButton("Zoom In (+)");
        detailZoomInButton.addActionListener(e -> {
            detailPanel.zoomIn();
            detailPanel.repaint();
        });

        JButton detailZoomOutButton = new JButton("Zoom Out (-)");
        detailZoomOutButton.addActionListener(e -> {
            detailPanel.zoomOut();
            detailPanel.repaint();
        });

        JButton detailZoomResetButton = new JButton("Reset Zoom");
        detailZoomResetButton.addActionListener(e -> {
            detailPanel.resetZoom();
            detailPanel.repaint();
        });

        zoomPanel.add(zoomLabel);
        zoomPanel.add(mainZoomInButton);
        zoomPanel.add(mainZoomOutButton);
        zoomPanel.add(mainZoomResetButton);
        zoomPanel.add(detailZoomLabel);
        zoomPanel.add(detailZoomInButton);
        zoomPanel.add(detailZoomOutButton);
        zoomPanel.add(detailZoomResetButton);

        return zoomPanel;
    }

    private JPanel createInfoPanel() {
        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        infoPanel.setBackground(new Color(30, 30, 30));

        infoLabel = new JLabel();
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(new Font("Monospace", Font.PLAIN, 11));

        infoPanel.add(infoLabel);

        return infoPanel;
    }

    private void togglePause() {
        isPaused = !isPaused;
        playPauseButton.setText(isPaused ? "Play" : "Pause");
    }

    private void spawnSpacecraft(int screenX, int screenY) {
        int centerX = 550;
        int centerY = 400;

        // Convert screen coordinates to solar system coordinates
        double x = (screenX - centerX) * Constants.IAU_2012_ASTRONOMICAL_UNIT / mainZoom;
        double y = (screenY - centerY) * Constants.IAU_2012_ASTRONOMICAL_UNIT / mainZoom;

        // Spawn with zero velocity
        engine.spawnSpacecraft(x, y);
        spacecraft = engine.getSpacecraft();

        System.out.println(String.format("Spawned spacecraft at (%.4f AU, %.4f AU)",
                x / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                y / Constants.IAU_2012_ASTRONOMICAL_UNIT));
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
            int x = (int)(centerX + pos.getX() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            List<Point> trajectory = trajectories.get(i);
            trajectory.add(new Point(x, y));

            if (trajectory.size() > MAX_TRAJECTORY_POINTS) {
                trajectory.remove(0);
            }
        }
    }

    private void updateSpacecraftTrajectories() {
        int centerX = 550;
        int centerY = 400;

        List<SpacecraftState> spacecraftList = engine.getSpacecraft();

        // Add new trajectory lists for newly spawned spacecraft
        while (spacecraftTrajectories.size() < spacecraftList.size()) {
            spacecraftTrajectories.add(new ArrayList<>());
        }

        for (int i = 0; i < spacecraftList.size(); i++) {
            Vector3D pos = spacecraftList.get(i).getPosition();
            int x = (int)(centerX + pos.getX() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            List<Point> trajectory = spacecraftTrajectories.get(i);
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
        for (List<Point> trajectory : spacecraftTrajectories) {
            trajectory.clear();
        }
    }

    private void updateLabels() {
        infoLabel.setText(String.format("Speed: %.2f days/frame | Elapsed: %.1f days | Main Zoom: %.1f",
                engine.getTimeSpeed(), engine.getElapsedDays(), mainZoom));

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

        // Draw spacecraft trajectories
        List<SpacecraftState> spacecraftList = spacecraft;
        for (int i = 0; i < spacecraftTrajectories.size() && i < spacecraftList.size(); i++) {
            List<Point> trajectory = spacecraftTrajectories.get(i);
            if (trajectory.size() > 1) {
                SpacecraftState sc = spacecraftList.get(i);
                g2d.setColor(new Color(sc.getColor().getRed(),
                        sc.getColor().getGreen(),
                        sc.getColor().getBlue(), 200));

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

        // Draw planets only (no moons in main view)
        for (PlanetState planet : planets) {
            Vector3D pos = planet.getPosition();
            int x = (int)(centerX + pos.getX() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            g2d.setColor(planet.getColor());
            int radius = planet.getRadius();
            g2d.fillOval(x - radius / 2, y - radius / 2, radius, radius);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 11));
            g2d.drawString(planet.getName(), x + radius, y);
        }

        // Draw spacecraft
        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        for (SpacecraftState sc : spacecraft) {
            Vector3D pos = sc.getPosition();
            int x = (int)(centerX + pos.getX() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            int y = (int)(centerY + pos.getY() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);

            g2d.setColor(sc.getColor());
            int size = 4;
            g2d.fillRect(x - size / 2, y - size / 2, size, size);

            g2d.setColor(Color.CYAN);
            g2d.drawString(sc.getName(), x + size, y);
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

                xPoints[i] = (int)(centerX + pos.getX() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
                yPoints[i] = (int)(centerY + pos.getY() * mainZoom / Constants.IAU_2012_ASTRONOMICAL_UNIT);
            }

            g2d.drawPolygon(xPoints, yPoints, numPoints);
        } catch (Exception e) {
            int orbitRadius = (int)(planet.getSemiMajorAxis() * mainZoom);
            g2d.drawOval(centerX - orbitRadius, centerY - orbitRadius,
                    orbitRadius * 2, orbitRadius * 2);
        }
    }
}
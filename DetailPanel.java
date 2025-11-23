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
 * Detail panel showing Earth-Moon and Mars-Phobos-Deimos systems
 */
public class DetailPanel extends JPanel {
    private OrebitEngine engine;
    private OrebitSimulationPanel parentPanel;
    private double earthMoonZoom = 0.00025;  // km to pixels scale
    private double marsZoom = 0.005;  // km to pixels scale
    private List<List<Point>> earthSpacecraftTrajectories;
    private List<List<Point>> marsSpacecraftTrajectories;
    private static final int MAX_TRAJECTORY_POINTS = 200;

    public DetailPanel(OrebitEngine engine, OrebitSimulationPanel parentPanel) {
        this.engine = engine;
        this.parentPanel = parentPanel;
        this.earthSpacecraftTrajectories = new ArrayList<>();
        this.marsSpacecraftTrajectories = new ArrayList<>();
        setBackground(Color.BLACK);
        setLayout(new GridLayout(2, 1, 5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Add mouse listener for spacecraft spawning
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    spawnSpacecraftAtClick(e.getX(), e.getY());
                }
            }
        });
    }

    public void zoomIn() {
        earthMoonZoom *= 1.5;
        marsZoom *= 1.5;
        if (earthMoonZoom > 20) earthMoonZoom = 20;
        if (marsZoom > 100) marsZoom = 100;
    }

    public void zoomOut() {
        earthMoonZoom /= 1.5;
        marsZoom /= 1.5;
        if (earthMoonZoom < 0.1) earthMoonZoom = 0.1;
        if (marsZoom < 0.5) marsZoom = 0.5;
    }

    public void resetZoom() {
        earthMoonZoom = 0.00025;
        marsZoom = 0.005;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int panelHeight = getHeight() / 2;

        // Update trajectories
        updateTrajectories();

        // Draw Earth-Moon system
        drawEarthMoonSystem(g2d, getWidth() / 2, panelHeight / 2);

        // Draw Mars-Phobos-Deimos system
        drawMarsSystem(g2d, getWidth() / 2, panelHeight + panelHeight / 2);
    }

    private void drawEarthMoonSystem(Graphics2D g2d, int centerX, int centerY) {
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Earth-Moon System", centerX - 70, 20);

        // Zoom indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("Zoom: %.2fx", earthMoonZoom / 0.00025), centerX + 100, 20);

        PlanetState earth = engine.getPlanetByName("Earth");
        if (earth == null) return;

        Vector3D earthPos = earth.getPosition();

        // Draw Moon's orbit
        MoonState moon = earth.getMoon();
        if (moon != null) {
            g2d.setColor(new Color(200, 200, 200, 80));
            g2d.setStroke(new BasicStroke(1));
            int moonOrbitRadius = (int) (moon.getSemiMajorAxis() / 1000.0 * earthMoonZoom);
            g2d.drawOval(centerX - moonOrbitRadius, centerY - moonOrbitRadius,
                    moonOrbitRadius * 2, moonOrbitRadius * 2);
        }

        // Draw spacecraft trajectories
        drawSpacecraftTrajectories(g2d, earthSpacecraftTrajectories);

        // Draw Earth
        g2d.setColor(new Color(100, 149, 237));
        int earthRadius = 25;
        g2d.fillOval(centerX - earthRadius / 2, centerY - earthRadius / 2, earthRadius, earthRadius);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Earth", centerX - 15, centerY + earthRadius);

        // Draw Moon at its actual position (relative to Earth)
        if (moon != null) {
            Vector3D moonRelPos = moon.getRelativePosition();

            // Debug output
            if (Math.random() < 0.01) { // Only print occasionally
                System.out.println("Moon relative position: " + moonRelPos.getNorm() / 1000.0 + " km");
            }

            int moonX = (int) (centerX + moonRelPos.getX() / 1000.0 * earthMoonZoom);
            int moonY = (int) (centerY + moonRelPos.getY() / 1000.0 * earthMoonZoom);

            g2d.setColor(new Color(200, 200, 200));
            int moonRadius = 8;
            g2d.fillOval(moonX - moonRadius / 2, moonY - moonRadius / 2, moonRadius, moonRadius);
            g2d.setColor(Color.WHITE);
            g2d.drawString("Moon", moonX + moonRadius, moonY);

            // Info
            g2d.setFont(new Font("Monospace", Font.PLAIN, 10));
            int infoY = centerY + 120;
            double moonDist = moonRelPos.getNorm() / 1000.0;
            g2d.drawString(String.format("Moon Distance: %.0f km", moonDist), 10, infoY);
            infoY += 15;
            g2d.drawString(String.format("Orbital Period: %.1f days", moon.getOrbitalPeriod() / 86400.0), 10, infoY);
        }

        // Draw spacecraft in this region
        drawSpacecraftInRegion(g2d, earthPos, earthMoonZoom, centerX, centerY, 1000000000.0);
    }

    private void drawMarsSystem(Graphics2D g2d, int centerX, int centerY) {
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Mars-Phobos-Deimos System", centerX - 100, centerY - 180);

        // Zoom indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("Zoom: %.2fx", marsZoom / 0.005), centerX + 100, centerY - 180);

        PlanetState mars = engine.getPlanetByName("Mars");
        if (mars == null) return;

        Vector3D marsPos = mars.getPosition();
        List<MoonState> moons = mars.getMoons();

        // Draw moon orbits
        if (moons != null && !moons.isEmpty()) {
            g2d.setStroke(new BasicStroke(1));
            for (MoonState moon : moons) {
                g2d.setColor(new Color(moon.getColor().getRed(),
                        moon.getColor().getGreen(),
                        moon.getColor().getBlue(), 80));
                int orbitRadius = (int) (moon.getSemiMajorAxis() / 1000.0 * marsZoom);
                g2d.drawOval(centerX - orbitRadius, centerY - orbitRadius,
                        orbitRadius * 2, orbitRadius * 2);
            }
        }

        // Draw spacecraft trajectories
        drawSpacecraftTrajectories(g2d, marsSpacecraftTrajectories);

        // Draw Mars
        g2d.setColor(new Color(205, 92, 92));
        int marsRadius = 20;
        g2d.fillOval(centerX - marsRadius / 2, centerY - marsRadius / 2, marsRadius, marsRadius);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Mars", centerX - 12, centerY + marsRadius);

        // Draw moons at their actual positions
        if (moons != null && !moons.isEmpty()) {
            for (MoonState moon : moons) {
                Vector3D moonPos = moon.getRelativePosition();
                int moonX = (int) (centerX + moonPos.getX() / 1000.0 * marsZoom);
                int moonY = (int) (centerY + moonPos.getY() / 1000.0 * marsZoom);

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
                double moonDist = moonPos.getNorm() / 1000.0;
                double period = moon.getOrbitalPeriod() / 3600.0;
                g2d.drawString(String.format("%s: %.0f km | Period: %.2f hrs",
                        moon.getName(), moonDist, period), 10, infoY);
                infoY += 15;
            }
        }

        // Draw spacecraft in this region
        drawSpacecraftInRegion(g2d, marsPos, marsZoom, centerX, centerY, 1000000000.0);
    }

    private void updateTrajectories() {
        int panelHeight = getHeight() / 2;
        int earthCenterX = getWidth() / 2;
        int earthCenterY = panelHeight / 2;
        int marsCenterX = getWidth() / 2;
        int marsCenterY = panelHeight + panelHeight / 2;

        PlanetState earth = engine.getPlanetByName("Earth");
        PlanetState mars = engine.getPlanetByName("Mars");

        if (earth == null || mars == null) return;

        Vector3D earthPos = earth.getPosition();
        Vector3D marsPos = mars.getPosition();

        List<SpacecraftState> spacecraftList = engine.getSpacecraft();

        // Add new trajectory lists for newly spawned spacecraft
        while (earthSpacecraftTrajectories.size() < spacecraftList.size()) {
            earthSpacecraftTrajectories.add(new ArrayList<>());
        }
        while (marsSpacecraftTrajectories.size() < spacecraftList.size()) {
            marsSpacecraftTrajectories.add(new ArrayList<>());
        }

        // Update Earth trajectories
        for (int i = 0; i < spacecraftList.size(); i++) {
            SpacecraftState sc = spacecraftList.get(i);
            Vector3D scPos = sc.getPosition();
            Vector3D relativeToEarth = scPos.subtract(earthPos);

            if (relativeToEarth.getNorm() < 1000000000.0) {
                int x = (int) (earthCenterX + relativeToEarth.getX() / 1000.0 * earthMoonZoom);
                int y = (int) (earthCenterY + relativeToEarth.getY() / 1000.0 * earthMoonZoom);

                List<Point> trajectory = earthSpacecraftTrajectories.get(i);
                trajectory.add(new Point(x, y));
                if (trajectory.size() > MAX_TRAJECTORY_POINTS) {
                    trajectory.remove(0);
                }
            }
        }

        // Update Mars trajectories
        for (int i = 0; i < spacecraftList.size(); i++) {
            SpacecraftState sc = spacecraftList.get(i);
            Vector3D scPos = sc.getPosition();
            Vector3D relativeToMars = scPos.subtract(marsPos);

            if (relativeToMars.getNorm() < 1000000000.0) {
                int x = (int) (marsCenterX + relativeToMars.getX() / 1000.0 * marsZoom);
                int y = (int) (marsCenterY + relativeToMars.getY() / 1000.0 * marsZoom);

                List<Point> trajectory = marsSpacecraftTrajectories.get(i);
                trajectory.add(new Point(x, y));
                if (trajectory.size() > MAX_TRAJECTORY_POINTS) {
                    trajectory.remove(0);
                }
            }
        }
    }

    private void drawSpacecraftTrajectories(Graphics2D g2d, List<List<Point>> trajectories) {
        g2d.setStroke(new BasicStroke(2));
        List<SpacecraftState> spacecraftList = engine.getSpacecraft();

        for (int i = 0; i < trajectories.size() && i < spacecraftList.size(); i++) {
            List<Point> trajectory = trajectories.get(i);
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
    }

    private void drawSpacecraftInRegion(Graphics2D g2d, Vector3D centralBody, double zoom,
                                        int centerX, int centerY, double maxDistance) {
        List<SpacecraftState> spacecraftList = engine.getSpacecraft();

        g2d.setFont(new Font("Arial", Font.PLAIN, 9));
        for (SpacecraftState sc : spacecraftList) {
            Vector3D scPos = sc.getPosition();
            Vector3D relativePos = scPos.subtract(centralBody);

            // Only draw if spacecraft is within range
            if (relativePos.getNorm() < maxDistance) {
                int x = (int) (centerX + relativePos.getX() / 1000.0 * zoom);
                int y = (int) (centerY + relativePos.getY() / 1000.0 * zoom);

                g2d.setColor(sc.getColor());
                int size = 4;
                g2d.fillRect(x - size / 2, y - size / 2, size, size);

                g2d.setColor(Color.CYAN);
                g2d.drawString(sc.getName(), x + size, y);
            }
        }
    }

    private void spawnSpacecraftAtClick(int screenX, int screenY) {
        int panelHeight = getHeight() / 2;

        // Determine which panel was clicked
        if (screenY < panelHeight) {
            // Earth-Moon panel
            spawnInEarthSystem(screenX, screenY, panelHeight / 2);
        } else {
            // Mars panel
            spawnInMarsSystem(screenX, screenY - panelHeight, panelHeight / 2);
        }
    }

    private void spawnInEarthSystem(int screenX, int screenY, int centerY) {
        PlanetState earth = engine.getPlanetByName("Earth");
        if (earth == null) return;

        int centerX = getWidth() / 2;

        // Convert screen coordinates to offset from Earth in meters
        double offsetX = (screenX - centerX) / earthMoonZoom * 1000.0; // meters
        double offsetY = (screenY - centerY) / earthMoonZoom * 1000.0;

        System.out.println(String.format("Detail panel click: screen(%d, %d) -> offset(%.0f km, %.0f km)",
                screenX, screenY, offsetX/1000.0, offsetY/1000.0));

        // Add Earth's position to get absolute solar system coordinates
        Vector3D earthPos = earth.getPosition();
        Vector3D position = new Vector3D(
                earthPos.getX() + offsetX,
                earthPos.getY() + offsetY,
                0
        );

        System.out.println(String.format("Earth position: (%.4f AU, %.4f AU)",
                earthPos.getX() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                earthPos.getY() / Constants.IAU_2012_ASTRONOMICAL_UNIT));
        System.out.println(String.format("Spawn position: (%.4f AU, %.4f AU)",
                position.getX() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                position.getY() / Constants.IAU_2012_ASTRONOMICAL_UNIT));

        parentPanel.spawnSpacecraftAtPosition(position);
        System.out.println("Spawned spacecraft near Earth");
    }

    private void spawnInMarsSystem(int screenX, int screenY, int centerY) {
        PlanetState mars = engine.getPlanetByName("Mars");
        if (mars == null) return;

        int centerX = getWidth() / 2;

        // Convert screen coordinates to offset from Mars in meters
        double offsetX = (screenX - centerX) / marsZoom * 1000.0; // meters
        double offsetY = (screenY - centerY) / marsZoom * 1000.0;

        System.out.println(String.format("Detail panel click: screen(%d, %d) -> offset(%.0f km, %.0f km)",
                screenX, screenY, offsetX/1000.0, offsetY/1000.0));

        // Add Mars's position to get absolute solar system coordinates
        Vector3D marsPos = mars.getPosition();
        Vector3D position = new Vector3D(
                marsPos.getX() + offsetX,
                marsPos.getY() + offsetY,
                0
        );

        System.out.println(String.format("Mars position: (%.4f AU, %.4f AU)",
                marsPos.getX() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                marsPos.getY() / Constants.IAU_2012_ASTRONOMICAL_UNIT));
        System.out.println(String.format("Spawn position: (%.4f AU, %.4f AU)",
                position.getX() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                position.getY() / Constants.IAU_2012_ASTRONOMICAL_UNIT));

        parentPanel.spawnSpacecraftAtPosition(position);
        System.out.println("Spawned spacecraft near Mars");
    }
}
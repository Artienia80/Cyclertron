import org.orekit.time.AbsoluteDate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

/**
 * Detail panel showing Earth-Moon and Mars-Phobos-Deimos systems
 */
public class DetailPanel extends JPanel {
    private OrebitEngine engine;
    private double earthMoonZoom = 0.5;  // Much lower to show moon orbit
    private double marsZoom = 3.0;  // Much lower to show both moon orbits

    public DetailPanel(OrebitEngine engine) {
        this.engine = engine;
        setBackground(Color.BLACK);
        setLayout(new GridLayout(2, 1, 5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
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
        earthMoonZoom = 0.5;
        marsZoom = 3.0;
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
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Earth-Moon System", centerX - 70, 20);

        // Zoom indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("Zoom: %.2fx", earthMoonZoom / 0.5), centerX + 100, 20);

        PlanetState earth = engine.getPlanetByName("Earth");
        if (earth == null) return;

        MoonState moon = earth.getMoon();
        if (moon == null) return;

        // Draw Moon's orbit
        g2d.setColor(new Color(200, 200, 200, 80));
        g2d.setStroke(new BasicStroke(1));
        int moonOrbitRadius = (int)(moon.getSemiMajorAxis() / 1000.0 * earthMoonZoom);
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
        int moonX = (int)(centerX + moonPos.getX() / 1000.0 * earthMoonZoom);
        int moonY = (int)(centerY + moonPos.getY() / 1000.0 * earthMoonZoom);

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
        // Title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Mars-Phobos-Deimos System", centerX - 100, centerY - 180);

        // Zoom indicator
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString(String.format("Zoom: %.2fx", marsZoom / 3.0), centerX + 100, centerY - 180);

        PlanetState mars = engine.getPlanetByName("Mars");
        if (mars == null) return;

        List<MoonState> moons = mars.getMoons();

        // Draw moon orbits
        g2d.setStroke(new BasicStroke(1));
        for (MoonState moon : moons) {
            g2d.setColor(new Color(moon.getColor().getRed(),
                    moon.getColor().getGreen(),
                    moon.getColor().getBlue(), 80));
            int orbitRadius = (int)(moon.getSemiMajorAxis() / 1000.0 * marsZoom);
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
            int moonX = (int)(centerX + moonPos.getX() / 1000.0 * marsZoom);
            int moonY = (int)(centerY + moonPos.getY() / 1000.0 * marsZoom);

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
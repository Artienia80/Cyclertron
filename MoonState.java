import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.awt.*;

/**
 * Represents a moon orbiting a planet
 */
public class MoonState {
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
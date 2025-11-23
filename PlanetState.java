import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a planet's state using Orekit propagation
 */
public class PlanetState {
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
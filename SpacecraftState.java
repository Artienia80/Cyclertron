import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.awt.*;

/**
 * Represents a spacecraft in free fall
 */
public class SpacecraftState {
    private String name;
    private NumericalPropagator propagator;
    private Vector3D position;
    private Vector3D velocity;
    private Color color;
    private double mass; // kg

    public SpacecraftState(String name, NumericalPropagator propagator, Color color, double mass) {
        this.name = name;
        this.propagator = propagator;
        this.color = color;
        this.mass = mass;
        this.position = new Vector3D(0, 0, 0);
        this.velocity = new Vector3D(0, 0, 0);
    }

    public void update(AbsoluteDate date) {
        try {
            var state = propagator.propagate(date);
            position = state.getPosition();
            velocity = state.getPVCoordinates().getVelocity();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getName() { return name; }
    public Vector3D getPosition() { return position; }
    public Vector3D getVelocity() { return velocity; }
    public Color getColor() { return color; }
    public double getMass() { return mass; }
}
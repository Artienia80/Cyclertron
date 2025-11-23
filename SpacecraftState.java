import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.awt.*;

/**
 * Represents a spacecraft in free fall using simple n-body physics
 */
public class SpacecraftState {
    private String name;
    private Vector3D position;
    private Vector3D velocity;
    private Vector3D acceleration;
    private Color color;
    private double mass; // kg
    private boolean destroyed;

    public SpacecraftState(String name, Vector3D position, Vector3D velocity, Color color, double mass) {
        this.name = name;
        this.position = position;
        this.velocity = velocity;
        this.acceleration = new Vector3D(0, 0, 0);
        this.color = color;
        this.mass = mass;
        this.destroyed = false;
    }

    public void update(double dt) {
        if (destroyed) return;

        // Update position and velocity using current acceleration
        // Velocity Verlet integration: v(t+dt) = v(t) + a(t) * dt
        velocity = velocity.add(acceleration.scalarMultiply(dt));

        // x(t+dt) = x(t) + v(t) * dt + 0.5 * a(t) * dt^2
        position = position.add(velocity.scalarMultiply(dt)).add(acceleration.scalarMultiply(0.5 * dt * dt));
    }

    public void computeAcceleration(Vector3D sunPos, double sunMass,
                                    Vector3D earthPos, double earthMass,
                                    Vector3D marsPos, double marsMass) {
        if (destroyed) return;

        final double G = 6.67430e-11; // Gravitational constant
        acceleration = new Vector3D(0, 0, 0);

        // Sun's gravity
        Vector3D rSun = sunPos.subtract(position);
        double distSun = rSun.getNorm();
        if (distSun > 0) {
            double aSun = G * sunMass / (distSun * distSun * distSun);
            acceleration = acceleration.add(rSun.scalarMultiply(aSun));
        }

        // Earth's gravity
        Vector3D rEarth = earthPos.subtract(position);
        double distEarth = rEarth.getNorm();
        if (distEarth > 0) {
            double aEarth = G * earthMass / (distEarth * distEarth * distEarth);
            acceleration = acceleration.add(rEarth.scalarMultiply(aEarth));
        }

        // Mars's gravity
        Vector3D rMars = marsPos.subtract(position);
        double distMars = rMars.getNorm();
        if (distMars > 0) {
            double aMars = G * marsMass / (distMars * distMars * distMars);
            acceleration = acceleration.add(rMars.scalarMultiply(aMars));
        }
    }

    public String getName() { return name; }
    public Vector3D getPosition() { return position; }
    public Vector3D getVelocity() { return velocity; }
    public Color getColor() { return color; }
    public double getMass() { return mass; }
    public boolean isDestroyed() { return destroyed; }
    public void setDestroyed(boolean destroyed) { this.destroyed = destroyed; }
}
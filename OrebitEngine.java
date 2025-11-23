import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.PVCoordinates;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.orekit.forces.gravity.NewtonianAttraction;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Physics engine using Orekit for accurate orbital propagation
 */
public class OrebitEngine {
    private Frame sunCentricFrame;
    private AbsoluteDate initialDate;
    private AbsoluteDate currentDate;
    private List<PlanetState> planets;
    private List<SpacecraftState> spacecraft;
    private double timeSpeed = 1.0;
    private double elapsedDays = 0;
    private int spacecraftCounter = 0;
    private Random colorRandom;

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

            spacecraft = new ArrayList<>();
            colorRandom = new Random();

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
            Frame earthFrame = FramesFactory.getEME2000();
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
            Frame marsFrame = FramesFactory.getEME2000();

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

    private void createSpacecraft() {
        // Removed - spacecraft are now spawned by user clicks
    }

    public void spawnSpacecraft(double x, double y) {
        try {
            spacecraftCounter++;

            // Create spacecraft at specified position with zero velocity
            Vector3D position = new Vector3D(x, y, 0);
            Vector3D velocity = new Vector3D(0, 0, 0);
            PVCoordinates pv = new PVCoordinates(position, velocity);

            // Create numerical propagator
            double minStep = 0.001;
            double maxStep = 1000.0;
            double positionTolerance = 10.0;
            DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                    minStep, maxStep, positionTolerance, positionTolerance);

            NumericalPropagator propagator = new NumericalPropagator(integrator);

            // Use Cartesian orbit type to avoid conversion issues
            propagator.setOrbitType(org.orekit.orbits.OrbitType.CARTESIAN);

            // Initialize with position and velocity (not orbit)
            org.orekit.orbits.CartesianOrbit cartesianOrbit =
                    new org.orekit.orbits.CartesianOrbit(
                            pv,
                            sunCentricFrame,
                            currentDate,
                            Constants.IAU_2015_NOMINAL_SUN_GM);

            org.orekit.propagation.SpacecraftState initialState =
                    new org.orekit.propagation.SpacecraftState(cartesianOrbit, 100.0); // 100 kg mass

            propagator.resetInitialState(initialState);

            // Add Sun's gravity
            propagator.addForceModel(new NewtonianAttraction(Constants.IAU_2015_NOMINAL_SUN_GM));

            // Random color
            Color color = new Color(
                    100 + colorRandom.nextInt(156),
                    100 + colorRandom.nextInt(156),
                    100 + colorRandom.nextInt(156)
            );

            SpacecraftState sc = new SpacecraftState("SC-" + spacecraftCounter, propagator, color, 100.0);
            sc.update(currentDate); // Initialize position
            spacecraft.add(sc);

            System.out.println("Spawned spacecraft SC-" + spacecraftCounter);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to spawn spacecraft: " + e.getMessage());
        }
    }

    public void update() {
        currentDate = currentDate.shiftedBy(timeSpeed * 86400.0);
        elapsedDays += timeSpeed;

        for (PlanetState planet : planets) {
            planet.update(currentDate);
        }

        for (SpacecraftState sc : spacecraft) {
            sc.update(currentDate);
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
        for (SpacecraftState sc : spacecraft) {
            sc.update(currentDate);
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
            for (SpacecraftState sc : spacecraft) {
                sc.update(currentDate);
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
            for (SpacecraftState sc : spacecraft) {
                sc.update(currentDate);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Invalid date", e);
        }
    }

    public List<PlanetState> getPlanets() { return planets; }
    public List<SpacecraftState> getSpacecraft() { return spacecraft; }
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
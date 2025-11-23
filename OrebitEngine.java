import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import java.awt.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Physics engine using Orekit for planets (on rails) and n-body physics for spacecraft
 */
public class OrebitEngine {
    private Frame sunCentricFrame;
    private AbsoluteDate initialDate;
    private AbsoluteDate currentDate;
    private List<PlanetState> planets;
    private List<SpacecraftState> spacecraft;
    private double timeSpeed = 1.0; // days per second (real-time)
    private double elapsedDays = 0;
    private int spacecraftCounter = 0;
    private Random colorRandom;

    // NASA JPL Keplerian Elements (J2000)
    private static final double[][] ELEMENTS = {
            {1.00000261, 0.01671123, -0.00001531, 100.46457166, 102.93768193, 0.0}, // Earth
            {1.52371034, 0.09339410, 1.84969142, -4.55343205, -23.94362959, 49.55953891}  // Mars
    };

    // Real masses (kg)
    private static final double SUN_MASS = 1.989e30;
    private static final double EARTH_MASS = 5.972e24;
    private static final double MARS_MASS = 6.39e23;

    public OrebitEngine() {
        try {
            System.out.println("Initializing orbital mechanics engine...");
            System.out.println("Planets on rails (Keplerian orbits), spacecraft use n-body physics");
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

            System.out.println("Created Earth with Moon (on Keplerian rails)");
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

            System.out.println("Created Mars with Phobos and Deimos (on Keplerian rails)");
            return mars;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to create Mars: " + e.getMessage());
            return null;
        }
    }

    public void spawnSpacecraft(Vector3D position, Vector3D velocity) {
        try {
            spacecraftCounter++;

            System.out.println(String.format("SC-%d spawned at (%.4f AU, %.4f AU) with velocity (%.2f, %.2f) km/s",
                    spacecraftCounter,
                    position.getX() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                    position.getY() / Constants.IAU_2012_ASTRONOMICAL_UNIT,
                    velocity.getX() / 1000.0,
                    velocity.getY() / 1000.0));

            // Random color
            Color color = new Color(
                    100 + colorRandom.nextInt(156),
                    100 + colorRandom.nextInt(156),
                    100 + colorRandom.nextInt(156)
            );

            SpacecraftState sc = new SpacecraftState("SC-" + spacecraftCounter, position, velocity, color, 100.0);
            spacecraft.add(sc);

            System.out.println("Successfully spawned SC-" + spacecraftCounter);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to spawn spacecraft: " + e.getMessage());
        }
    }

    public void update() {
        // Fixed timestep: 60 Hz = 0.016 seconds per frame
        double dtSeconds = 0.016;

        // dtSeconds is the real-time elapsed (0.016 for 60 Hz)
        // timeSpeed is in days per second
        double simulatedDays = timeSpeed * dtSeconds;
        double dt = simulatedDays * 86400.0; // Convert to seconds for physics

        currentDate = currentDate.shiftedBy(dt);
        elapsedDays += simulatedDays;

        // Update planets on their Keplerian rails (unaffected by spacecraft)
        for (PlanetState planet : planets) {
            planet.update(currentDate);
        }

        // N-body physics for spacecraft - they feel gravity from everything
        Vector3D sunPos = new Vector3D(0, 0, 0); // Sun at origin
        Vector3D earthPos = planets.get(0).getPosition();
        Vector3D marsPos = planets.get(1).getPosition();

        // Compute accelerations for all spacecraft from all gravitational sources
        for (SpacecraftState sc : spacecraft) {
            if (!sc.isDestroyed()) {
                sc.computeAcceleration(sunPos, SUN_MASS, earthPos, EARTH_MASS, marsPos, MARS_MASS);
            }
        }

        // Update spacecraft positions and velocities
        for (SpacecraftState sc : spacecraft) {
            if (!sc.isDestroyed()) {
                sc.update(dt);
            }
        }

        // Check for collisions
        List<SpacecraftState> toRemove = new ArrayList<>();
        for (SpacecraftState sc : spacecraft) {
            if (sc.isDestroyed()) {
                toRemove.add(sc);
                continue;
            }

            // Check collision with Sun (radius ~696,000 km)
            double distanceToSun = sc.getPosition().getNorm();
            if (distanceToSun < 696000000.0) { // 696,000 km in meters
                sc.setDestroyed(true);
                toRemove.add(sc);
                System.out.println(sc.getName() + " destroyed by collision with Sun!");
                continue;
            }

            // Check collision with planets
            for (PlanetState planet : planets) {
                Vector3D relativePos = sc.getPosition().subtract(planet.getPosition());
                double distanceToPlanet = relativePos.getNorm();

                double planetRadius;
                if (planet.getName().equals("Earth")) {
                    planetRadius = 6371000.0; // Earth radius in meters
                } else if (planet.getName().equals("Mars")) {
                    planetRadius = 3389500.0; // Mars radius in meters
                } else {
                    planetRadius = 1000000.0; // Default 1000 km
                }

                if (distanceToPlanet < planetRadius) {
                    sc.setDestroyed(true);
                    toRemove.add(sc);
                    System.out.println(sc.getName() + " destroyed by collision with " + planet.getName() + "!");
                    break;
                }
            }
        }

        // Remove destroyed spacecraft
        spacecraft.removeAll(toRemove);
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
        // Keep spacecraft as-is (don't reset them)
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
            // Spacecraft continue from their current positions
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
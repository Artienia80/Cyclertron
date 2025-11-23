import org.orekit.data.DataContext;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DirectoryCrawler;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Cyclertron - High-Fidelity Orbital Mechanics Simulator using Orekit
 * Main entry point that initializes Orekit and launches the application
 */
public class Cyclertron extends JFrame {
    private OrebitSimulationPanel simulationPanel;

    public Cyclertron() {
        setTitle("Cyclertron - Orekit Orbital Mechanics Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set to fullscreen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(false); // Keep window decorations for easy closing

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
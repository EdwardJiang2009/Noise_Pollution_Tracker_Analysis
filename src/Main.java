import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        // Launch the GUI on the Event Dispatch Thread (Swing's rule for thread safety)
        SwingUtilities.invokeLater(() -> {
            new NoiseTrackerApp().setVisible(true);
        });
    }
}

import gui.MainWindow;
import gui.VideoGenerationWorker;

import javax.swing.*;

/**
 * Application entry point.
 *
 * <p>Launches the Swing GUI on the Event Dispatch Thread and wires the
 * Generate button to a background {@link VideoGenerationWorker}. All heavy
 * processing runs off the EDT so the window — including the loading bar —
 * stays fully responsive.</p>
 */
public class Main {

    /**
     * Starts the application.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // Native window decorations where available
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        SwingUtilities.invokeLater(() -> {
            MainWindow window = new MainWindow();

            // Wire the Generate button to the background worker
            window.setOnGenerate(inputFolder -> {
                VideoGenerationWorker worker = new VideoGenerationWorker(inputFolder, window);

                // Forward SwingWorker progress (0-100) to the window's progress bar
                worker.addPropertyChangeListener(evt -> {
                    if ("progress".equals(evt.getPropertyName())) {
                        int pct = (int) evt.getNewValue();
                        window.setProgress(pct, "Processing… " + pct + "%");
                    }
                });

                worker.execute();
            });

            window.setVisible(true);
        });
    }
}

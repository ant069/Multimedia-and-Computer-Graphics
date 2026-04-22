package gui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Main application window for the AI Travel Video Generator.
 *
 * <p>Provides a dark-themed GUI with drag-and-drop file input, a folder selector,
 * a progress bar, a log area, and a Generate button. All heavy work is delegated
 * to a background {@link SwingWorker} so the UI stays responsive.</p>
 */
public class MainWindow extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG       = new Color(18, 18, 24);
    private static final Color SURFACE  = new Color(28, 28, 38);
    private static final Color ACCENT   = new Color(99, 102, 241);
    private static final Color ACCENT2  = new Color(139, 92, 246);
    private static final Color TEXT     = new Color(226, 232, 240);
    private static final Color SUBTEXT  = new Color(148, 163, 184);
    private static final Color SUCCESS  = new Color(34, 197, 94);
    private static final Color ERROR    = new Color(239, 68, 68);
    private static final Color DROP_BG  = new Color(38, 38, 52);

    // ── State ─────────────────────────────────────────────────────────────────
    private File inputFolder;
    private final List<File> droppedFiles = new ArrayList<>();

    // ── Widgets ───────────────────────────────────────────────────────────────
    private JLabel dropLabel;
    private JPanel dropZone;
    private JTextField folderField;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private JTextArea logArea;
    private JButton generateButton;

    /** Consumer called when the user clicks Generate; receives the resolved input folder path. */
    private Consumer<String> onGenerate;

    /**
     * Creates and displays the main window.
     */
    public MainWindow() {
        super("AI Travel Video Generator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(720, 640));
        setPreferredSize(new Dimension(820, 720));
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout(0, 0));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    // ── Builder helpers ───────────────────────────────────────────────────────

    /** Builds the top title bar. */
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        JLabel title = new JLabel("✈  AI Travel Video Generator");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setForeground(TEXT);
        panel.add(title, BorderLayout.WEST);

        return panel;
    }

    /** Builds the central content area (drop zone + folder picker + log). */
    private JPanel buildCenter() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        panel.add(buildDropZone(), BorderLayout.NORTH);
        panel.add(buildFolderRow(), BorderLayout.CENTER);
        panel.add(buildLogArea(), BorderLayout.SOUTH);

        return panel;
    }

    /** Builds the drag-and-drop target panel. */
    private JPanel buildDropZone() {
        dropZone = new JPanel(new BorderLayout());
        dropZone.setBackground(DROP_BG);
        dropZone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2, true),
                new EmptyBorder(40, 20, 40, 20)));
        dropZone.setPreferredSize(new Dimension(0, 120));

        dropLabel = new JLabel("<html><center>Drop photos/videos here<br>or select a folder below</center></html>");
        dropLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        dropLabel.setForeground(SUBTEXT);
        dropLabel.setHorizontalAlignment(SwingConstants.CENTER);
        dropZone.add(dropLabel, BorderLayout.CENTER);

        // Enable drag-and-drop
        new DropTarget(dropZone, new DropTargetAdapter() {
            @Override
            public void dragOver(DropTargetDragEvent dtde) {
                dropZone.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT2, 3, true),
                        new EmptyBorder(40, 20, 40, 20)));
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                dropZone.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 2, true),
                        new EmptyBorder(40, 20, 40, 20)));
            }

            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    handleDroppedFiles(files);
                } catch (Exception e) {
                    log("Error handling dropped files: " + e.getMessage(), ERROR);
                }
            }
        });

        return dropZone;
    }

    /** Builds the manual folder-picker row. */
    private JPanel buildFolderRow() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setBackground(BG);

        JLabel label = new JLabel("Or select folder:");
        label.setForeground(TEXT);
        panel.add(label, BorderLayout.WEST);

        folderField = new JTextField();
        folderField.setBackground(SURFACE);
        folderField.setForeground(TEXT);
        folderField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 65), 1),
                new EmptyBorder(8, 12, 8, 12)));
        panel.add(folderField, BorderLayout.CENTER);

        JButton browseButton = styledButton("Browse", ACCENT);
        browseButton.addActionListener(e -> chooseFolderAction());
        panel.add(browseButton, BorderLayout.EAST);

        return panel;
    }

    /** Builds the scrollable log text area. */
    private JScrollPane buildLogArea() {
        logArea = new JTextArea();
        logArea.setBackground(SURFACE);
        logArea.setForeground(TEXT);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(0, 200));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(55, 55, 65), 1));

        return scroll;
    }

    /** Builds the bottom bar with progress bar, status label and Generate button. */
    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout(16, 0));
        panel.setBackground(SURFACE);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));

        // Left: progress bar + status
        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.setBackground(SURFACE);

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(ACCENT);
        progressBar.setBackground(new Color(55, 55, 65));
        progressBar.setPreferredSize(new Dimension(300, 20));
        left.add(progressBar, BorderLayout.NORTH);

        statusLabel = new JLabel("Ready");
        statusLabel.setForeground(SUBTEXT);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        left.add(statusLabel, BorderLayout.SOUTH);

        panel.add(left, BorderLayout.WEST);

        // Right: Generate button
        generateButton = styledButton("Generate Video", SUCCESS);
        generateButton.addActionListener(e -> triggerGenerate());
        panel.add(generateButton, BorderLayout.EAST);

        return panel;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Opens a folder chooser dialog and stores the selection. */
    private void chooseFolderAction() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputFolder = chooser.getSelectedFile();
            folderField.setText(inputFolder.getAbsolutePath());
            log("Selected folder: " + inputFolder.getName());
        }
    }

    /**
     * Handles files dropped onto the drop zone.
     * If a single directory is dropped it is used as the input folder; otherwise the
     * parent directory of the first file is used.
     *
     * @param files the dropped files
     */
    private void handleDroppedFiles(List<File> files) {
        droppedFiles.clear();
        droppedFiles.addAll(files);

        if (files.size() == 1 && files.get(0).isDirectory()) {
            inputFolder = files.get(0);
            folderField.setText(inputFolder.getAbsolutePath());
            log("Dropped folder: " + inputFolder.getName());
        } else {
            File parent = files.get(0).getParentFile();
            inputFolder = parent;
            folderField.setText(parent.getAbsolutePath());
            log("Dropped " + files.size() + " file(s), using parent folder: " + parent.getName());
        }

        dropLabel.setText("<html><center>✓ Files ready<br>Click Generate to start</center></html>");
        dropLabel.setForeground(SUCCESS);
    }

    /** Validates input and fires the generate callback on a background thread. */
    private void triggerGenerate() {
        if (inputFolder == null || !inputFolder.exists()) {
            log("Please select a valid input folder first.", ERROR);
            return;
        }

        if (onGenerate != null) {
            setGenerating(true);
            onGenerate.accept(inputFolder.getAbsolutePath());
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Registers the callback that is invoked when the user clicks Generate.
     *
     * @param handler a consumer that receives the resolved input folder path
     */
    public void setOnGenerate(Consumer<String> handler) {
        this.onGenerate = handler;
    }

    /**
     * Updates the progress bar and status label. Safe to call from any thread.
     *
     * @param percent  value 0-100
     * @param message  short status message shown below the bar
     */
    public void setProgress(int percent, String message) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setValue(percent);
            statusLabel.setText(message);
        });
    }

    /**
     * Appends a line to the log area. Safe to call from any thread.
     *
     * @param message the text to append
     * @param color   the foreground color for this line
     */
    public void log(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Appends a plain white line to the log area.
     *
     * @param message the text to append
     */
    public void log(String message) {
        log(message, TEXT);
    }

    /**
     * Marks generation as finished and re-enables the Generate button.
     *
     * @param success true if the video was created successfully
     * @param message final status message
     */
    public void onGenerationComplete(boolean success, String message) {
        SwingUtilities.invokeLater(() -> {
            setGenerating(false);
            log(message, success ? SUCCESS : ERROR);
            setProgress(100, success ? "Complete" : "Failed");
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Enables or disables the Generate button and toggles the progress bar indeterminate state.
     *
     * @param generating true while generation is in progress
     */
    private void setGenerating(boolean generating) {
        generateButton.setEnabled(!generating);
        progressBar.setIndeterminate(generating);
        if (generating) {
            progressBar.setString("Processing…");
        }
    }

    /**
     * Creates a styled rounded button.
     *
     * @param label      the button text
     * @param background the background color
     * @return the configured button
     */
    private JButton styledButton(String label, Color background) {
        JButton button = new JButton(label);
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setPreferredSize(new Dimension(140, 36));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }
}

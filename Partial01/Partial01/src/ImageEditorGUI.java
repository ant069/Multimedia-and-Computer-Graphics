import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import operations.CropOperation;
import operations.InvertOperation;
import operations.RotateOperation;


public class ImageEditorGUI extends JFrame {
    private ImageEditor editor;
    private BufferedImage previewImage;
    private JLabel imageLabel;
    private JFileChooser fileChooser;
    private String loadedPath;

    public ImageEditorGUI() {
        super("Image Editor");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Image display
        imageLabel = new JLabel("No image loaded", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(800, 600));
        JScrollPane scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        // Controls
        JPanel controls = new JPanel();
        JButton loadBtn = new JButton("Load Image");
        JButton cropBtn = new JButton("Crop");
        JButton invertBtn = new JButton("Invert");
        JButton rotateBtn = new JButton("Rotate");
        JButton clearBtn = new JButton("Clear Pipeline");
        JButton previewBtn = new JButton("Preview Pipeline");
        JButton saveBtn = new JButton("Save As");
        controls.add(loadBtn);
        controls.add(cropBtn);
        controls.add(invertBtn);
        controls.add(rotateBtn);
        controls.add(clearBtn);
        controls.add(previewBtn);
        controls.add(saveBtn);
        add(controls, BorderLayout.SOUTH);

        fileChooser = new JFileChooser();

        // Button actions
        loadBtn.addActionListener(e -> loadImage());
        cropBtn.addActionListener(e -> cropImage());
        invertBtn.addActionListener(e -> invertImage());
        rotateBtn.addActionListener(e -> rotateImage());
        clearBtn.addActionListener(e -> clearPipeline());
        previewBtn.addActionListener(e -> previewPipeline());
        saveBtn.addActionListener(e -> saveImage());
    }

    private void loadImage() {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                editor = new ImageEditor(file.getAbsolutePath());
                loadedPath = file.getAbsolutePath();
                previewImage = ImageIO.read(file);
                updateImage(previewImage);
            } catch (IOException ex) {
                showError("Failed to load image: " + ex.getMessage());
            }
        }
    }

    private void cropImage() {
        if (editor == null) return;
        int[] rect = promptRect("Crop region");
        if (rect == null) return;
        editor.addOperation(new CropOperation(rect[0], rect[1], rect[2], rect[3]));
        showInfo("Crop operation added to pipeline.");
    }

    private void invertImage() {
        if (editor == null) return;
        int[] rect = promptRect("Invert region");
        if (rect == null) return;
        editor.addOperation(new InvertOperation(rect[0], rect[1], rect[2], rect[3]));
        showInfo("Invert operation added to pipeline.");
    }

    private void rotateImage() {
        if (editor == null) return;
        int[] rect = promptRect("Rotate region");
        if (rect == null) return;
        String[] options = {"90", "180", "270"};
        String degStr = (String) JOptionPane.showInputDialog(this, "Degrees:", "Rotate", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (degStr == null) return;
        int deg = Integer.parseInt(degStr);
        editor.addOperation(new RotateOperation(rect[0], rect[1], rect[2], rect[3], deg));
        showInfo("Rotate operation added to pipeline.");
    }

    private void clearPipeline() {
        if (editor == null) return;
        editor.clearOperations();
        showInfo("Pipeline cleared.");
    }

    private void previewPipeline() {
        if (editor == null) return;
        try {
            BufferedImage img = getPreviewImage();
            updateImage(img);
        } catch (Exception ex) {
            showError("Error previewing pipeline: " + ex.getMessage());
        }
    }

    private void saveImage() {
        if (editor == null) return;
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                editor.save(file.getAbsolutePath());
                showInfo("Image saved: " + file.getName());
            } catch (IOException ex) {
                showError("Failed to save image: " + ex.getMessage());
            }
        }
    }

    private int[] promptRect(String title) {
        JPanel panel = new JPanel(new GridLayout(2, 4));
        JTextField x1 = new JTextField(), y1 = new JTextField(), x2 = new JTextField(), y2 = new JTextField();
        panel.add(new JLabel("x1:")); panel.add(x1);
        panel.add(new JLabel("y1:")); panel.add(y1);
        panel.add(new JLabel("x2:")); panel.add(x2);
        panel.add(new JLabel("y2:")); panel.add(y2);
        int res = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return null;
        try {
            return new int[] {
                Integer.parseInt(x1.getText()),
                Integer.parseInt(y1.getText()),
                Integer.parseInt(x2.getText()),
                Integer.parseInt(y2.getText())
            };
        } catch (NumberFormatException e) {
            showError("Invalid coordinates.");
            return null;
        }
    }

    private BufferedImage getPreviewImage() throws IOException {
        if (editor == null) return null;
        // Apply all operations to a copy
        java.util.List<operations.ImageOperation> ops = new java.util.ArrayList<>(editor.pipeline);
        BufferedImage img = ImageIO.read(new File(loadedPath));
        for (operations.ImageOperation op : ops) {
            img = op.apply(img);
        }
        return img;
    }

    private void updateImage(BufferedImage img) {
        if (img == null) return;
        Image scaled = img.getScaledInstance(imageLabel.getWidth(), imageLabel.getHeight(), Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaled));
        imageLabel.setText("");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageEditorGUI().setVisible(true));
    }
}

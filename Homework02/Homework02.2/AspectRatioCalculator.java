
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Scanner;

public class AspectRatioCalculator extends JFrame {
    private JTextField widthField;
    private JTextField heightField;
    private JLabel resultLabel;
    private JButton calculateButton;
    private JButton fileButton;

    public AspectRatioCalculator() {
        setTitle("Aspect Ratio Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(5, 2, 5, 5));

        add(new JLabel("Width:"));
        widthField = new JTextField();
        add(widthField);

        add(new JLabel("Height:"));
        heightField = new JTextField();
        add(heightField);

        calculateButton = new JButton("Calculate");
        add(calculateButton);

        fileButton = new JButton("Load from File");
        add(fileButton);

        resultLabel = new JLabel("Aspect Ratio: ");
        add(resultLabel);

        calculateButton.addActionListener(e -> calculateAspectRatio());
        fileButton.addActionListener(e -> loadFromFile());
    }

    private void calculateAspectRatio() {
        try {
            int width = Integer.parseInt(widthField.getText());
            int height = Integer.parseInt(heightField.getText());
            int gcd = gcd(width, height);
            resultLabel.setText("Aspect Ratio: " + (width / gcd) + ":" + (height / gcd));
        } catch (NumberFormatException ex) {
            resultLabel.setText("Invalid input");
        }
    }

    private void loadFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (Scanner scanner = new Scanner(file)) {
                if (scanner.hasNextInt()) {
                    widthField.setText(String.valueOf(scanner.nextInt()));
                }
                if (scanner.hasNextInt()) {
                    heightField.setText(String.valueOf(scanner.nextInt()));
                }
                resultLabel.setText("Loaded from file");
            } catch (Exception ex) {
                resultLabel.setText("Error reading file");
            }
        }
    }

    private int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AspectRatioCalculator().setVisible(true);
        });
    }
}

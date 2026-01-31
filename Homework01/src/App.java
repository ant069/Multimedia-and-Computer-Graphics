import javax.swing.*;
import java.awt.*;

public class App {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculatorFrame());
    }
}

class CalculatorFrame extends JFrame {
    private JComboBox<String> shapeSelector;
    private JPanel inputPanel;
    private JLabel resultLabel;

    public CalculatorFrame() {
        setTitle("Area & Perimeter Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);

        String[] shapes = {"Square", "Rectangle", "Triangle", "Circle", "Regular Pentagon"};
        shapeSelector = new JComboBox<>(shapes);
        inputPanel = new JPanel();
        resultLabel = new JLabel("Select a shape and enter dimensions.");

        shapeSelector.addActionListener(e -> updateInputFields());
        JButton calcButton = new JButton("Calculate");
        calcButton.addActionListener(e -> calculate());

        setLayout(new BorderLayout());
        add(shapeSelector, BorderLayout.NORTH);
        add(inputPanel, BorderLayout.CENTER);
        add(calcButton, BorderLayout.SOUTH);
        add(resultLabel, BorderLayout.PAGE_END);

        updateInputFields();
        setVisible(true);
    }

    private void updateInputFields() {
        inputPanel.removeAll();
        String shape = (String) shapeSelector.getSelectedItem();
        inputPanel.setLayout(new GridLayout(3, 2, 5, 5));

        
        java.util.function.Supplier<JTextField> fieldSupplier = () -> {
            JTextField field = new JTextField(10);
            field.addActionListener(e -> calculate());
            return field;
        };

        switch (shape) {
            case "Square":
                inputPanel.add(new JLabel("Side:"));
                inputPanel.add(fieldSupplier.get());
                break;
            case "Rectangle":
                inputPanel.add(new JLabel("Length:"));
                inputPanel.add(fieldSupplier.get());
                inputPanel.add(new JLabel("Width:"));
                inputPanel.add(fieldSupplier.get());
                break;
            case "Triangle":
                inputPanel.add(new JLabel("Base:"));
                inputPanel.add(fieldSupplier.get());
                inputPanel.add(new JLabel("Height:"));
                inputPanel.add(fieldSupplier.get());
                inputPanel.add(new JLabel("Side A:"));
                inputPanel.add(fieldSupplier.get());
                inputPanel.add(new JLabel("Side B:"));
                inputPanel.add(fieldSupplier.get());
                inputPanel.add(new JLabel("Side C:"));
                inputPanel.add(fieldSupplier.get());
                break;
            case "Circle":
                inputPanel.add(new JLabel("Radius:"));
                inputPanel.add(fieldSupplier.get());
                break;
            case "Regular Pentagon":
                inputPanel.add(new JLabel("Side:"));
                inputPanel.add(fieldSupplier.get());
                break;
        }
        inputPanel.revalidate();
        inputPanel.repaint();
    }

    private void calculate() {
        String shape = (String) shapeSelector.getSelectedItem();
        Component[] components = inputPanel.getComponents();
        try {
            double area = 0, perimeter = 0;
            switch (shape) {
                case "Square": {
                    double side = Double.parseDouble(((JTextField) components[1]).getText());
                    area = side * side;
                    perimeter = 4 * side;
                    break;
                }
                case "Rectangle": {
                    double length = Double.parseDouble(((JTextField) components[1]).getText());
                    double width = Double.parseDouble(((JTextField) components[3]).getText());
                    area = length * width;
                    perimeter = 2 * (length + width);
                    break;
                }
                case "Triangle": {
                    double base = Double.parseDouble(((JTextField) components[1]).getText());
                    double height = Double.parseDouble(((JTextField) components[3]).getText());
                    double a = Double.parseDouble(((JTextField) components[5]).getText());
                    double b = Double.parseDouble(((JTextField) components[7]).getText());
                    double c = Double.parseDouble(((JTextField) components[9]).getText());
                    area = 0.5 * base * height;
                    perimeter = a + b + c;
                    break;
                }
                case "Circle": {
                    double radius = Double.parseDouble(((JTextField) components[1]).getText());
                    area = Math.PI * radius * radius;
                    perimeter = 2 * Math.PI * radius;
                    break;
                }
                case "Regular Pentagon": {
                    double side = Double.parseDouble(((JTextField) components[1]).getText());
                    area = (5 * side * side) / (4 * Math.tan(Math.PI / 5));
                    perimeter = 5 * side;
                    break;
                }
            }
            resultLabel.setText(String.format("Area: %.2f, Perimeter: %.2f", area, perimeter));
        } catch (Exception ex) {
            resultLabel.setText("Please enter valid numbers for all fields.");
        }
    }
}

package Homework02.Homework02_3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class CoordinateConverter extends JFrame {
    private JTextField xField, yField, rField, thetaField;
    private JButton toPolarButton, toCartesianButton, plotRoseButton;
    private JLabel polarResult, cartesianResult;
    private JPanel graphPanel;
    private JTextField aField, kField, y0Field;

    public CoordinateConverter() {
        setTitle("Coordinate Converter & Polar Rose");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JPanel inputPanel = new JPanel(new GridLayout(6, 4, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Coordinate Conversion"));

        inputPanel.add(new JLabel("x:"));
        xField = new JTextField();
        inputPanel.add(xField);
        inputPanel.add(new JLabel("y:"));
        yField = new JTextField();
        inputPanel.add(yField);
        toPolarButton = new JButton("To Polar");
        inputPanel.add(toPolarButton);
        polarResult = new JLabel("r=, θ=");
        inputPanel.add(polarResult);

        inputPanel.add(new JLabel("r:"));
        rField = new JTextField();
        inputPanel.add(rField);
        inputPanel.add(new JLabel("θ (deg):"));
        thetaField = new JTextField();
        inputPanel.add(thetaField);
        toCartesianButton = new JButton("To Cartesian");
        inputPanel.add(toCartesianButton);
        cartesianResult = new JLabel("x=, y=");
        inputPanel.add(cartesianResult);

        inputPanel.add(new JLabel("a (rose):"));
        aField = new JTextField("100");
        inputPanel.add(aField);
        inputPanel.add(new JLabel("k (int, rose):"));
        kField = new JTextField("4");
        inputPanel.add(kField);
        inputPanel.add(new JLabel("y0 (deg, rose):"));
        y0Field = new JTextField("0");
        inputPanel.add(y0Field);
        plotRoseButton = new JButton("Plot Polar Rose");
        inputPanel.add(plotRoseButton);

        add(inputPanel, BorderLayout.NORTH);

        graphPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawRose(g);
            }
        };
        graphPanel.setPreferredSize(new Dimension(600, 350));
        graphPanel.setBackground(Color.WHITE);
        add(graphPanel, BorderLayout.CENTER);

        toPolarButton.addActionListener(e -> convertToPolar());
        toCartesianButton.addActionListener(e -> convertToCartesian());
        plotRoseButton.addActionListener(e -> graphPanel.repaint());
    }

    private void convertToPolar() {
        try {
            double x = Double.parseDouble(xField.getText());
            double y = Double.parseDouble(yField.getText());
            double r = Math.hypot(x, y);
            double theta = Math.toDegrees(Math.atan2(y, x));
            polarResult.setText(String.format("r=%.2f, θ=%.2f°", r, theta));
        } catch (NumberFormatException ex) {
            polarResult.setText("Invalid input");
        }
    }

    private void convertToCartesian() {
        try {
            double r = Double.parseDouble(rField.getText());
            double theta = Math.toRadians(Double.parseDouble(thetaField.getText()));
            double x = r * Math.cos(theta);
            double y = r * Math.sin(theta);
            cartesianResult.setText(String.format("x=%.2f, y=%.2f", x, y));
        } catch (NumberFormatException ex) {
            cartesianResult.setText("Invalid input");
        }
    }

    private void drawRose(Graphics g) {
        try {
            double a = Double.parseDouble(aField.getText());
            int k = Integer.parseInt(kField.getText());
            double y0 = Math.toRadians(Double.parseDouble(y0Field.getText()));
            int w = graphPanel.getWidth();
            int h = graphPanel.getHeight();
            int cx = w / 2;
            int cy = h / 2;
            int points = 1000;
            int[] xs = new int[points];
            int[] ys = new int[points];
            for (int i = 0; i < points; i++) {
                double phi = 2 * Math.PI * i / points;
                double r = a * Math.cos(k * phi + y0);
                int x = (int) (cx + r * Math.cos(phi));
                int y = (int) (cy - r * Math.sin(phi));
                xs[i] = x;
                ys[i] = y;
            }
            g.setColor(Color.BLUE);
            for (int i = 1; i < points; i++) {
                g.drawLine(xs[i - 1], ys[i - 1], xs[i], ys[i]);
            }
        } catch (Exception ex) {
            // Ignore drawing if input is invalid
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CoordinateConverter().setVisible(true);
        });
    }
}

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Classwork02_1 {
    public static void main(String[] args) {
        drawBarycentricTriangle("barycentric_triangle", "png");
    }

    public static void drawBarycentricTriangle(String filename, String fileType) {
        int width = 800;
        int height = 800;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int[] x = {100, width - 100, width / 2};
        int[] y = {height - 100, height - 100, 100};

        for (int px = 0; px < width; px++) {
            for (int py = 0; py < height; py++) {
                double[] bary = barycentric(px, py, x, y);
                if (bary[0] >= 0 && bary[1] >= 0 && bary[2] >= 0) {
                    int r = (int) (bary[0] * 255 + bary[1] * 0 + bary[2] * 0);
                    int g = (int) (bary[0] * 0 + bary[1] * 255 + bary[2] * 0);
                    int b = (int) (bary[0] * 0 + bary[1] * 0 + bary[2] * 255);
                    Color color = new Color(r, g, b);
                    image.setRGB(px, py, color.getRGB());
                } else {
                    image.setRGB(px, py, Color.BLACK.getRGB());
                }
            }
        }

        saveImage(image, filename, fileType);
    }

    public static double[] barycentric(int px, int py, int[] x, int[] y) {
        double detT = (y[1] - y[2]) * (x[0] - x[2]) + (x[2] - x[1]) * (y[0] - y[2]);
        double l1 = ((y[1] - y[2]) * (px - x[2]) + (x[2] - x[1]) * (py - y[2])) / detT;
        double l2 = ((y[2] - y[0]) * (px - x[2]) + (x[0] - x[2]) * (py - y[2])) / detT;
        double l3 = 1.0 - l1 - l2;
        return new double[]{l1, l2, l3};
    }

    public static void saveImage(BufferedImage image, String fileName, String fileType) {
        File file = new File(fileName + "." + fileType);
        try {
            ImageIO.write(image, fileType, file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

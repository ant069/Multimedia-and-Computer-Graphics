import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Classwork01_4 {
    public static void main(String[] args) {
        drawClasswork01("Classwork01_4/classwork01_image", "png");
    }

    public static void drawClasswork01(String filename, String fileType) {
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Draw vertical gradient background (yellow to red)
        for (int y = 0; y < height; y++) {
            float ratio = (float) y / height;
            int red = (int) (255 * ratio + 255 * (1 - ratio)); // yellow to red
            int green = (int) (255 * (1 - ratio)); // yellow to 0
            int blue = 0;
            Color blendedColor = new Color(red, green, blue);
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, blendedColor.getRGB());
            }
        }

        int figureSpacing = width / 4;
        int figureY = 300;
        for (int i = 0; i < 3; i++) {
            int centerX = figureSpacing * (i + 1);
            drawStylizedFigure(image, centerX, figureY);
        }

        saveImage(image, filename, fileType);
    }

    public static void drawStylizedFigure(BufferedImage image, int centerX, int centerY) {
        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);

        g.fillOval(centerX - 30, centerY - 120, 40, 40);
        int[] xPoints = {centerX - 40, centerX, centerX + 40};
        int[] yPoints = {centerY - 80, centerY + 40, centerY - 80};
        g.fillPolygon(xPoints, yPoints, 3);
 
        int[] xPoints2 = {centerX - 30, centerX, centerX + 30};
        int[] yPoints2 = {centerY + 40, centerY + 120, centerY + 40};
        g.fillPolygon(xPoints2, yPoints2, 3);

        g.fillRect(centerX - 20, centerY + 120, 10, 40);
        g.fillRect(centerX + 10, centerY + 120, 10, 40);
 
        g.fillOval(centerX + 5, centerY - 135, 20, 20);
        g.dispose();
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

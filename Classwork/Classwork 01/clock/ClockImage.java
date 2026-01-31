import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ClockImage {
    public static void main(String[] args) {
        int width = 800;
        int height = 600;
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = Math.min(width, height) * 2 / 5;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        fillBackground(image, Color.BLACK);

        drawCircle(image, centerX, centerY, radius, Color.WHITE);

        for (int i = 1; i <= 12; i++) {
            double angle = Math.toRadians(i * 30 - 90);
            int dotRadius = radius - 15;
            int x = (int) (centerX + Math.cos(angle) * dotRadius);
            int y = (int) (centerY + Math.sin(angle) * dotRadius);
            
            drawDot(image, x, y, Color.WHITE, 2);
        }

        int hour = 10;
        int minute = 5;

        double hourAngle = Math.toRadians((hour + minute / 60.0) * 30 - 90);
        int hourX = (int) (centerX + Math.cos(hourAngle) * (radius * 0.45));
        int hourY = (int) (centerY + Math.sin(hourAngle) * (radius * 0.45));
        drawThickLine(image, centerX, centerY, hourX, hourY, Color.WHITE, 3);

        double minuteAngle = Math.toRadians(minute * 6 - 90);
        int minX = (int) (centerX + Math.cos(minuteAngle) * (radius * 0.7));
        int minY = (int) (centerY + Math.sin(minuteAngle) * (radius * 0.7));
        drawThickLine(image, centerX, centerY, minX, minY, Color.WHITE, 3);

        File outputImage = new File("clock_clean.jpg");
        try {
            ImageIO.write(image, "jpg", outputImage);
            System.out.println("Clock image created: " + outputImage.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void fillBackground(BufferedImage img, Color color) {
        int rgb = color.getRGB();
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void drawCircle(BufferedImage img, int cx, int cy, int r, Color color) {
        int rgb = color.getRGB();
        for (int angle = 0; angle < 360; angle++) {
            double rad = Math.toRadians(angle);
            int x = (int) Math.round(cx + Math.cos(rad) * r);
            int y = (int) Math.round(cy + Math.sin(rad) * r);
            if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void drawDot(BufferedImage img, int cx, int cy, Color color, int size) {
        int rgb = color.getRGB();
        for (int dx = -size; dx <= size; dx++) {
            for (int dy = -size; dy <= size; dy++) {
                if (dx*dx + dy*dy <= size*size) {
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x >= 0 && x < img.getWidth() && y >= 0 && y < img.getHeight()) {
                        img.setRGB(x, y, rgb);
                    }
                }
            }
        }
    }

    private static void drawThickLine(BufferedImage img, int x0, int y0, int x1, int y1, Color color, int thickness) {
        int rgb = color.getRGB();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            for (int offsetX = -thickness/2; offsetX <= thickness/2; offsetX++) {
                for (int offsetY = -thickness/2; offsetY <= thickness/2; offsetY++) {
                    int px = x0 + offsetX;
                    int py = y0 + offsetY;
                    if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                        img.setRGB(px, py, rgb);
                    }
                }
            }
            
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class SunLandscape {
    public static void main(String[] args) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        fillBackground(image, Color.WHITE);

        int sunCenterX = 115;
        int sunCenterY = 90;
        int sunRadius = 60;

        drawRays(image, sunCenterX, sunCenterY, sunRadius, new Color(180, 180, 180));
        
        fillCircle(image, sunCenterX, sunCenterY, sunRadius, new Color(255, 255, 0));

        int grassStartY = 430;
        drawWavyGrass(image, grassStartY, new Color(0, 255, 0));

        File outputImage = new File("sun_landscape.jpg");
        try {
            ImageIO.write(image, "jpg", outputImage);
            System.out.println("Sun landscape created: " + outputImage.getAbsolutePath());
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

    private static void fillCircle(BufferedImage img, int cx, int cy, int radius, Color color) {
        int rgb = color.getRGB();
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x*x + y*y <= radius*radius) {
                    int px = cx + x;
                    int py = cy + y;
                    if (px >= 0 && px < img.getWidth() && py >= 0 && py < img.getHeight()) {
                        img.setRGB(px, py, rgb);
                    }
                }
            }
        }
    }

    private static void drawRays(BufferedImage img, int cx, int cy, int sunRadius, Color color) {
        int rayLength = 50;
        for (int i = 0; i < 8; i++) {
            double angle = Math.toRadians(i * 45);
            int startX = (int) (cx + Math.cos(angle) * (sunRadius + 5));
            int startY = (int) (cy + Math.sin(angle) * (sunRadius + 5));
            int endX = (int) (cx + Math.cos(angle) * (sunRadius + rayLength + 5));
            int endY = (int) (cy + Math.sin(angle) * (sunRadius + rayLength + 5));
            drawLine(img, startX, startY, endX, endY, color);
        }
    }

    private static void drawWavyGrass(BufferedImage img, int startY, Color color) {
        int rgb = color.getRGB();
        int width = img.getWidth();
        int height = img.getHeight();
        int waveCount = 7;
        int waveWidth = width / waveCount;
        int amplitude = 70;
        int[] waveHeights = new int[width];
        
        for (int i = 0; i < waveCount; i++) {
            for (int x = i * waveWidth; x < (i + 1) * waveWidth && x < width; x++) {
                double t = (double)(x - i * waveWidth) / waveWidth;
                double y = Math.sin(t * Math.PI) * amplitude;
                waveHeights[x] = (int) (startY - y);
            }
        }
        
        for (int x = 0; x < width; x++) {
            for (int y = waveHeights[x]; y < height; y++) {
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void drawLine(BufferedImage img, int x0, int y0, int x1, int y1, Color color) {
        int rgb = color.getRGB();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        
        while (true) {
            if (x0 >= 0 && x0 < img.getWidth() && y0 >= 0 && y0 < img.getHeight()) {
                img.setRGB(x0, y0, rgb);
            }
            
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}

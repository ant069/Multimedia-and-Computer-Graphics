import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Classwork01_1 {
    public static void main(String[] args) {
        drawDiagonal("classwork01_1_image", "png");
    }

    public static void drawDiagonal(String filename, String fileType) {
        int width = 800;
        int height = 600;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // Divide la imagen diagonalmente: arriba-izquierda rojo, abajo-derecha azul
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (y < (-0.75 * x + height)) {
                    image.setRGB(x, y, Color.RED.getRGB());
                } else {
                    image.setRGB(x, y, Color.BLUE.getRGB());
                }
            }
        }

        saveImage(image, "Classwork01_1/" + filename, fileType);
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

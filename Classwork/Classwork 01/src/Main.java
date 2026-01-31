import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class Main {
    public static void main(String[] args) {
        int width = 800;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                

                if (y > (double) height / width * x) {
                    image.setRGB(x, y, Color.BLUE.getRGB());
                } else {
                    image.setRGB(x, y, Color.RED.getRGB());
                }
            }
        }
        File outputImage = new File("classwork01.jpg");
        try {
            ImageIO.write(image, "jpg", outputImage);
            System.out.println("Image created: " + outputImage.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
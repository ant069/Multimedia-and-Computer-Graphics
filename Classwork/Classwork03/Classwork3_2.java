import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Classwork3_2 {
    public static void main(String[] args) {
        createSVG("Classwork03/Classwork3_2.svg", 800, 600);
    }

    public static void createSVG(String filePath, int width, int height) {
        StringBuilder svgContent = new StringBuilder();
        svgContent.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        svgContent.append("<svg width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\" xmlns=\"http://www.w3.org/2000/svg\">\n");

        // Fondo blanco
        svgContent.append("<rect width=\"" + width + "\" height=\"" + height + "\" fill=\"white\"/>\n");

        // Sol amarillo
        int sunRadius = 80;
        int sunCenterX = 200;
        int sunCenterY = 150;
        svgContent.append("<circle cx=\"" + sunCenterX + "\" cy=\"" + sunCenterY + "\" r=\"" + sunRadius + "\" fill=\"yellow\"/>\n");

        // Rayos del sol (4 principales + 4 diagonales)
        int rayLength = 120;
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI / 4 * i;
            int x1 = sunCenterX + (int)(Math.cos(angle) * sunRadius);
            int y1 = sunCenterY + (int)(Math.sin(angle) * sunRadius);
            int x2 = sunCenterX + (int)(Math.cos(angle) * rayLength);
            int y2 = sunCenterY + (int)(Math.sin(angle) * rayLength);
            svgContent.append("<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"red\" stroke-width=\"2\"/>\n");
        }

        // Césped verde (onda senoidal)
        svgContent.append("<path d=\"M 0 " + (height - 100));
        int waveAmplitude = 50;
        int waveLength = 100;
        for (int x = 0; x <= width; x += 10) {
            double y = height - 100 + waveAmplitude * Math.sin(2 * Math.PI * x / waveLength);
            svgContent.append(" L " + x + " " + (int)y);
        }
        svgContent.append(" L " + width + " " + height);
        svgContent.append(" L 0 " + height);
        svgContent.append(" Z\" fill=\"lime\"/>\n");

        svgContent.append("</svg>");

        File file = new File(filePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(svgContent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import java.io.FileWriter;
import java.io.IOException;
import java.io.File;

public class Classwork3_1 {
    public static void main(String[] args) {
        createSVG("Classwork03/Classwork3_1.svg", 800, 600);
    }

    public static void createSVG(String filePath, int width, int height) {
        String svgContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<svg width=\"" + width + "\" height=\"" + height + "\" viewBox=\"0 0 " + width + " " + height + "\" xmlns=\"http://www.w3.org/2000/svg\">\n" +
                
                "  <polygon points=\"0,0 " + width + ",0 " + width + "," + height + "\" fill=\"red\"/>\n" +
                
                "  <polygon points=\"0,0 0," + height + " " + width + "," + height + "\" fill=\"blue\"/>\n" +
                "</svg>";
        File file = new File(filePath);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(svgContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import java.io.*;

public class Homework3_2 {
    // Simple compression algorithm: RLE (Run-Length Encoding) for PGM images (grayscale)
    // You can adapt to other formats if needed

    // Compress a PGM file using RLE
    public static void compressImage(String inputPath, String outputPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputPath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

        String line;
        // Copy PGM header
        for (int i = 0; i < 3; i++) {
            line = reader.readLine();
            writer.write(line);
            writer.newLine();
        }

        // Compress pixel data
        int prev = -1, count = 0;
        while ((line = reader.readLine()) != null) {
            String[] pixels = line.trim().split("\\s+");
            for (String pixel : pixels) {
                int val = Integer.parseInt(pixel);
                if (val == prev) {
                    count++;
                } else {
                    if (prev != -1) {
                        writer.write(prev + "," + count + " ");
                    }
                    prev = val;
                    count = 1;
                }
            }
        }
        // Write last group
        if (prev != -1) {
            writer.write(prev + "," + count);
        }
        writer.close();
        reader.close();
    }

    // Decompress a PGM file compressed with RLE
    public static void decompressImage(String inputPath, String outputPath) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(inputPath));
        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));

        String line;
        // Copy PGM header
        for (int i = 0; i < 3; i++) {
            line = reader.readLine();
            writer.write(line);
            writer.newLine();
        }

        // Decompress pixel data
        while ((line = reader.readLine()) != null) {
            String[] groups = line.trim().split("\\s+");
            for (String group : groups) {
                String[] parts = group.split(",");
                int val = Integer.parseInt(parts[0]);
                int count = Integer.parseInt(parts[1]);
                for (int i = 0; i < count; i++) {
                    writer.write(val + " ");
                }
            }
            writer.newLine();
        }
        writer.close();
        reader.close();
    }

    public static void main(String[] args) {
        // Usage example:
        // compressImage("original.pgm", "compressed.pgm");
        // decompressImage("compressed.pgm", "decompressed.pgm");
        System.out.println("Usage: java Homework3_2 <compress|decompress> <input> <output>");
        if (args.length != 3) return;
        try {
            if (args[0].equalsIgnoreCase("compress")) {
                compressImage(args[1], args[2]);
                System.out.println("Image compressed successfully.");
            } else if (args[0].equalsIgnoreCase("decompress")) {
                decompressImage(args[1], args[2]);
                System.out.println("Image decompressed successfully.");
            } else {
                System.out.println("Unknown command.");
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}

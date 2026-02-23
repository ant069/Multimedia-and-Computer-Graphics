
import operations.CropOperation;
import operations.InvertOperation;
import operations.RotateOperation;

import java.io.IOException;
import java.util.Scanner;


public class Main {

    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        printBanner();

        // ── Load image ────────────────────────────────────────────────
        ImageEditor editor = null;
        while (editor == null) {
            System.out.print("\nEnter image path: ");
            String path = sc.nextLine().trim();
            try {
                editor = new ImageEditor(path);
            } catch (IOException | IllegalArgumentException e) {
                System.out.println("  Error: " + e.getMessage());
            }
        }

        // ── Operation loop ────────────────────────────────────────────
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> doCrop(editor);
                case "2" -> doInvert(editor);
                case "3" -> doRotate(editor);
                case "4" -> editor.previewPipeline();
                case "5" -> editor.clearOperations();
                case "6" -> {
                    doSave(editor);
                    running = false;
                }
                case "0" -> {
                    System.out.println("  Exiting without saving.");
                    running = false;
                }
                default  -> System.out.println("  Invalid option.");
            }
        }
    }

    // ── Operation handlers ────────────────────────────────────────────────

    private static void doCrop(ImageEditor editor) {
        System.out.println("  Crop – enter two corner coordinates:");
        int x1 = promptInt("    x1: ");
        int y1 = promptInt("    y1: ");
        int x2 = promptInt("    x2: ");
        int y2 = promptInt("    y2: ");
        editor.addOperation(new CropOperation(x1, y1, x2, y2));
    }

    private static void doInvert(ImageEditor editor) {
        System.out.println("  Invert – enter the rectangular region:");
        int x1 = promptInt("    x1: ");
        int y1 = promptInt("    y1: ");
        int x2 = promptInt("    x2: ");
        int y2 = promptInt("    y2: ");
        editor.addOperation(new InvertOperation(x1, y1, x2, y2));
    }

    private static void doRotate(ImageEditor editor) {
        System.out.println("  Rotate – enter the region and angle:");
        int x1 = promptInt("    x1: ");
        int y1 = promptInt("    y1: ");
        int x2 = promptInt("    x2: ");
        int y2 = promptInt("    y2: ");
        int deg;
        while (true) {
            deg = promptInt("    Degrees (90 / 180 / 270): ");
            if (deg == 90 || deg == 180 || deg == 270) break;
            System.out.println("  Must be 90, 180, or 270.");
        }
        editor.addOperation(new RotateOperation(x1, y1, x2, y2, deg));
    }

    private static void doSave(ImageEditor editor) {
        System.out.print("  Output filename (e.g. result.png): ");
        String out = sc.nextLine().trim();
        try {
            editor.save(out);
        } catch (IOException e) {
            System.out.println("  Error saving file: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static int promptInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a whole number.");
            }
        }
    }

    private static void printBanner() {
        System.out.println("=".repeat(60));
        System.out.println("  Multimedia & Computer Graphics – Image Editor");
        System.out.println("  Universidad Panamericana | 2026");
        System.out.println("=".repeat(60));
    }

    private static void printMenu() {
        System.out.println("""

        Options:
          1 – Crop image  (two corner coordinates)
          2 – Invert colors in a region
          3 – Rotate a region  (90 / 180 / 270°)
          4 – Show pipeline
          5 – Clear pipeline
          6 – Save and exit
          0 – Exit without saving
        """);
        System.out.print("Choice: ");
    }
}
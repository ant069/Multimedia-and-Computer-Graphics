# Image Editor – GUI Version

## Requirements
- Java 17 or higher (JDK)

## Compilation

1. Open a terminal in the `Partial01` folder (where the `src` folder is located).
2. Compile all source files:

   ```sh
   javac -d bin src\*.java src\operations\*.java
   ```

## Running the GUI

1. In the terminal, run:

   ```sh
   java -cp bin ImageEditorGUI
   ```

## Usage
- Click **Load Image** to open an image file.
- Use the buttons to apply **Crop**, **Invert**, or **Rotate** (you can combine them in any order and as many times as you want).
- Click **Preview Pipeline** to see the temporary result.
- Click **Save As** to save the result (the original image is never overwritten).
- **Clear Pipeline** removes all pending operations.

## Notes
- The program works with PNG, JPG, BMP, and other common image formats.
- If you have issues with file paths, use absolute paths or move the image to the project folder.

---

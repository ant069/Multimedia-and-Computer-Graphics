
# Partial01 – Java Image Editor

## Overview
This project is a Java image editor for the Multimedia & Computer Graphics course at Universidad Panamericana (2026). It allows users to load an image, apply multiple operations (crop, invert colors, rotate) in any order and any number of times, and save the result as a new image file. The original image is never overwritten.

## Features
- **Crop:** Select a rectangular region by two corner coordinates and crop the image.
- **Invert Colors:** Select a rectangular region and invert every pixel inside it (R, G, B channels).
- **Rotate:** Select a rectangular region and rotate it clockwise by 90°, 180°, or 270°. Gaps are filled with black.
- **Operation Pipeline:** Apply any combination of operations, in any order, multiple times before saving.
- **Save:** Generates a new image file with all operations applied.

## Usage
1. Run the program.
2. Enter the path to the image file you want to edit.
3. Use the menu to add crop, invert, or rotate operations. You can preview or clear the pipeline.
4. When finished, save the result as a new image file.

## Code Principles
- Clean OOP design: Each operation is a class, all inherit from `ImageOperation`.
- No unnecessary code, flexible and scalable pipeline.
- Well-documented and readable code.

## Structure
- `src/`: Java source files
	- `Main.java`: Entry point and menu
	- `ImageEditor.java`: Pipeline and file management
	- `operations/`: CropOperation, InvertOperation, RotateOperation, ImageOperation (base)
- `bin/`: Compiled classes
- `lib/`: Dependencies (if any)

=======

>>>>>>> efff603 (Initial commit: Java image editor with GUI, crop, invert, rotate, and pipeline support)

## Requirements
- Java 17+ (recommended)
- VS Code or any Java IDE

## How to Run

javac -d bin src/Main.java src/ImageEditor.java src/operations/*.java
<<<<<<< HEAD
java -cp bin Main
```


=======
java -cp bin Main
>>>>>>> efff603 (Initial commit: Java image editor with GUI, crop, invert, rotate, and pipeline support)

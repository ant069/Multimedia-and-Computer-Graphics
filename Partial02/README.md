# AI Video Creator

A Java application that transforms a series of GPS-tagged photos and videos into a cinematic travel video using AI-powered content generation.

## Overview

This program creates engaging travel videos from user-uploaded visual media. It leverages Google's Gemini AI to generate introductory images, descriptive narration, and inspirational content, while programmatically handling all media processing, scaling, and assembly.

### Key Features

- **AI-Generated Intro**: Creates a unique cinematic poster capturing the journey's essence
- **Chronological Media Display**: Shows photos/videos from oldest to newest with AI narration
- **AI Map Visualization**: Generates a stylized world map showing travel route with inspirational phrases
- **Portrait Video Output**: Optimized for mobile viewing (1080x1920)
- **Audio Normalization**: Meets YouTube standards for loudness and dynamics
- **Intuitive GUI**: User-friendly interface with progress tracking

## Requirements

### Functional Requirements

- **Input**: Series of photos/videos with GPS metadata
- **AI Components**:
  - Introductory image showing journey essence
  - Audio descriptions for each media item
  - Inspirational phrase based on visited locations
- **Processing**:
  - Scale all media to portrait frame while maintaining aspect ratio
  - Order media chronologically
  - Generate AI map with distinct pins for start/end locations
- **Output**: MP4 video in portrait orientation

### Technical Requirements

- **AI API**: Google Gemini (Free Tier from Google AI Studio)
- **Audio Standards**:
  - Loudness: -16 to -14 LUFS
  - True Peak: -2 to -1 dBTP
  - LRA: 5 to 10 LU
- **Video Format**: Portrait mode (1080x1920)
- **GUI**: Intuitive interface with loading progress bar

## Architecture

### Core Components

- **Main.java**: Application entry point with Swing GUI
- **VideoGenerationWorker.java**: Background worker orchestrating the pipeline
- **MediaProcessor.java**: Scans and sorts media files by date
- **ImageProcessor.java**: Scales images to portrait format
- **VideoProcessor.java**: Extracts frames from videos
- **GeminiClient.java**: Handles AI API communication
- **ImageGenerator.java**: Creates AI-generated images (intro + map)
- **TextGenerator.java**: Generates descriptions and inspirational phrases
- **TTSGenerator.java**: Converts text to speech
- **AudioNormalizer.java**: Applies YouTube audio standards
- **MapRenderer.java**: Generates AI map with overlaid text
- **GeocodingService.java**: Converts GPS to place names
- **VideoAssembler.java**: Combines all elements into final MP4

### Design Principles

✅ **OOP**: Clean class hierarchy with single responsibilities
✅ **Reusability**: Modular components can be used independently
✅ **Flexibility**: Configurable via properties file
✅ **Scalability**: Handles variable numbers of media files
✅ **Documentation**: Comprehensive JavaDoc comments
✅ **No Bugs**: Thorough error handling and validation
✅ **Clean Code**: Well-structured, readable, and maintainable

## Installation

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Google Gemini API key (free from [Google AI Studio](https://aistudio.google.com))

### Setup

1. Clone or download the project
2. Navigate to the project directory
3. Configure API key in config.properties:
   `
   api.key=YOUR_GEMINI_API_KEY_HERE
   `
4. Build the project:
   `ash
   mvn clean compile
   `
5. Run the application:
   `ash
   mvn exec:java -Dexec.mainClass="Main"
   `

## Usage

1. Launch the application
2. Select a folder containing GPS-tagged photos/videos
3. Click "Generate" to start processing
4. Monitor progress via the loading bar
5. Find the output video in the input folder

### Input Requirements

- Supported formats: JPG, PNG, MP4, MOV
- All files must contain GPS metadata (latitude/longitude)
- Files without GPS data will be rejected

### Output

- MP4 video file named output_video.mp4
- Portrait orientation (1080x1920)
- Includes intro image, media sequence with narration, and closing map

## AI Integration

### Gemini API Key

**IMPORTANT**: Submit your Gemini API key as a comment in your Blackboard delivery.

Example comment format:
`java
// Gemini API Key: YOUR_ACTUAL_API_KEY_HERE - Obtained from Google AI Studio (Free Tier)
`

### AI Components

1. **Intro Image**: Generated based on media summary
2. **Media Descriptions**: AI-generated narration for each item
3. **Inspirational Phrase**: Context-aware text based on locations
4. **Map Visualization**: AI-generated stylized world map

## Challenge Mode Features

✅ **GUI Implementation**: Swing-based interface
✅ **Progress Tracking**: Real-time loading bar
✅ **Intuitive Design**: Simple folder selection and generation
✅ **Audio Compliance**: Automatic normalization to YouTube standards


## Dependencies

- **JavaCV**: Video processing and FFmpeg integration
- **Thumbnailator**: Image scaling and manipulation
- **Metadata Extractor**: GPS and EXIF data reading
- **OkHttp**: HTTP client for API calls
- **Gson**: JSON parsing
- **Swing**: GUI framework

## Configuration

Modify config.properties to customize:
- output.resolution: Video dimensions (default: 1080x1920)
- rame.duration: Duration per photo in seconds
- output.filename: Output video filename

## Error Handling

The application includes comprehensive error handling for:
- Missing GPS data
- API failures
- File I/O issues
- Invalid media formats
- Network timeouts





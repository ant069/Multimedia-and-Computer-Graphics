package processor;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.mp4.Mp4MetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.exif.ExifExifSubIFDDirectory;
import com.drew.metadata.mov.media.QuickTimeVideoDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import model.MediaFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Scans folders, extracts metadata from supported media files, and returns them sorted by date.
 */
public class MediaProcessor {
    /**
     * Scans a folder for supported media files, extracts metadata, and sorts the result in ascending date order.
     *
     * @param folderPath the folder containing the input media files
     * @return a sorted list of media files
     * @throws IOException if the folder cannot be read or metadata extraction fails
     */
    public List<MediaFile> scanAndSort(String folderPath) throws IOException {
        Path folder = Path.of(folderPath);
        if (!Files.isDirectory(folder)) {
            throw new IOException("Input path is not a directory: " + folderPath);
        }

        List<MediaFile> mediaFiles = new ArrayList<>();
        try (Stream<Path> stream = Files.list(folder)) {
            stream.filter(Files::isRegularFile)
                .filter(this::isSupportedMedia)
                .map(Path::toFile)
                .forEach(file -> mediaFiles.add(readMediaFile(file)));
        }

        mediaFiles.sort(Comparator.comparing(MediaFile::getDate));
        return mediaFiles;
    }

    /**
     * Checks whether a file extension is supported by the processor.
     *
     * @param path the path to check
     * @return true when the extension is supported
     */
    private boolean isSupportedMedia(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
            || name.endsWith(".mp4") || name.endsWith(".mov");
    }

    /**
     * Builds a {@link MediaFile} instance from a physical file.
     *
     * @param file the file to inspect
     * @return the populated media file
     */
    private MediaFile readMediaFile(File file) {
        String type = isVideo(file) ? "VIDEO" : "PHOTO";
        Metadata metadata = readMetadata(file, type);
        LocalDateTime date = extractDate(metadata, file);
        double[] gps = extractGps(metadata);
        int orientation = extractOrientation(metadata);
        return new MediaFile(file, type, date, gps[0], gps[1], orientation);
    }

    /**
     * Determines whether a file should be treated as a video.
     *
     * @param file the file to inspect
     * @return true when the file is a supported video
     */
    private boolean isVideo(File file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp4") || name.endsWith(".mov");
    }

    /**
     * Reads metadata using the appropriate extractor for the file type.
     *
     * @param file the file to inspect
     * @param type the resolved media type
     * @return the metadata object, or an empty one if extraction fails
     */
    private Metadata readMetadata(File file, String type) {
        try {
            if ("VIDEO".equals(type)) {
                return Mp4MetadataReader.readMetadata(file);
            }
            return ImageMetadataReader.readMetadata(file);
        } catch (Exception exception) {
            return new Metadata();
        }
    }

    /**
     * Extracts the most relevant date from the metadata.
     *
     * @param metadata the metadata to inspect
     * @param file the original file
     * @return the resolved date, never null
     */
    private LocalDateTime extractDate(Metadata metadata, File file) {
        Date date = null;

        ExifSubIFDDirectory ExifSubIFDDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (ExifSubIFDDirectory != null) {
            date = firstNonNullDate(
                ExifSubIFDDirectory.getDateOriginal(),
                ExifSubIFDDirectory.getDateDigitized(),
                ExifSubIFDDirectory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL)
            );
        }

        if (date == null) {
            ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Directory != null) {
                date = ifd0Directory.getDate(ExifIFD0Directory.TAG_DATETIME);
            }
        }

        if (date == null) {
            Mp4Directory mp4Directory = metadata.getFirstDirectoryOfType(Mp4Directory.class);
            if (mp4Directory != null) {
                date = mp4Directory.getDate(Mp4Directory.TAG_CREATION_TIME);
            }
        }

        if (date == null) {
            QuickTimeVideoDirectory quickTimeDirectory = metadata.getFirstDirectoryOfType(QuickTimeVideoDirectory.class);
            if (quickTimeDirectory != null) {
                date = quickTimeDirectory.getDate(QuickTimeVideoDirectory.TAG_CREATION_TIME);
            }
        }

        if (date == null) {
            date = new Date(file.lastModified());
        }

        return LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault());
    }

    /**
     * Returns the first non-null date in the argument list.
     *
     * @param dates candidate dates
     * @return the first non-null date or null when none exist
     */
    private Date firstNonNullDate(Date... dates) {
        for (Date date : dates) {
            if (date != null) {
                return date;
            }
        }
        return null;
    }

    /**
     * Extracts the latitude and longitude from the metadata.
     *
     * @param metadata the metadata to inspect
     * @return a two-element array containing latitude and longitude
     */
    private double[] extractGps(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null) {
            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
            if (geoLocation != null && !geoLocation.isZero()) {
                return new double[]{geoLocation.getLatitude(), geoLocation.getLongitude()};
            }
        }
        return new double[]{0.0d, 0.0d};
    }

    /**
     * Extracts the EXIF orientation when present.
     *
     * @param metadata the metadata to inspect
     * @return the EXIF orientation value, or 1 when unavailable
     */
    private int extractOrientation(Metadata metadata) {
        ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (ifd0Directory != null) {
            Integer orientation = ifd0Directory.getInteger(ExifIFD0Directory.TAG_ORIENTATION);
            if (orientation != null) {
                return orientation;
            }
        }

        for (Directory directory : metadata.getDirectories()) {
            for (Tag tag : directory.getTags()) {
                if (tag.getTagName().toLowerCase(Locale.ROOT).contains("rotation")) {
                    try {
                        String description = directory.getDescription(tag.getTagType());
                        if (description != null) {
                            return Integer.parseInt(description.replaceAll("[^0-9-]", ""));
                        }
                    } catch (Exception ignored) {
                        return 1;
                    }
                }
            }
        }

        return 1;
    }
}

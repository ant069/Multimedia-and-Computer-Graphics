package model;

import java.io.File;
import java.time.LocalDateTime;

/**
 * Represents a photo or video file together with its extracted metadata.
 */
public class MediaFile {
    private File file;
    private String type;
    private LocalDateTime date;
    private double gpsLat;
    private double gpsLon;
    private int orientation;

    /**
     * Creates a media file instance.
     *
     * @param file the media file on disk
     * @param type the media type, usually PHOTO or VIDEO
     * @param date the metadata date associated with the file
     * @param gpsLat the latitude extracted from metadata
     * @param gpsLon the longitude extracted from metadata
     * @param orientation the EXIF orientation value
     */
    public MediaFile(File file, String type, LocalDateTime date, double gpsLat, double gpsLon, int orientation) {
        this.file = file;
        this.type = type;
        this.date = date;
        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;
        this.orientation = orientation;
    }

    /**
     * Returns the media file.
     *
     * @return the file on disk
     */
    public File getFile() {
        return file;
    }

    /**
     * Updates the media file.
     *
     * @param file the new file value
     */
    public void setFile(File file) {
        this.file = file;
    }

    /**
     * Returns the media type.
     *
     * @return PHOTO or VIDEO
     */
    public String getType() {
        return type;
    }

    /**
     * Updates the media type.
     *
     * @param type the new type value
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns the metadata date.
     *
     * @return the date associated with the media
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Updates the metadata date.
     *
     * @param date the new date value
     */
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    /**
     * Returns the latitude.
     *
     * @return the latitude value or 0.0 if unavailable
     */
    public double getGpsLat() {
        return gpsLat;
    }

    /**
     * Updates the latitude.
     *
     * @param gpsLat the new latitude value
     */
    public void setGpsLat(double gpsLat) {
        this.gpsLat = gpsLat;
    }

    /**
     * Returns the longitude.
     *
     * @return the longitude value or 0.0 if unavailable
     */
    public double getGpsLon() {
        return gpsLon;
    }

    /**
     * Updates the longitude.
     *
     * @param gpsLon the new longitude value
     */
    public void setGpsLon(double gpsLon) {
        this.gpsLon = gpsLon;
    }

    /**
     * Returns the EXIF orientation.
     *
     * @return the EXIF orientation value
     */
    public int getOrientation() {
        return orientation;
    }

    /**
     * Updates the EXIF orientation.
     *
     * @param orientation the new orientation value
     */
    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }
}

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Loads configuration values from config.properties.
 */
public class Config {
    private static Properties properties = new Properties();
    static {
        try {
            properties.load(new FileInputStream("config.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load config.properties", e);
        }
    }
    public static String get(String key) {
        return properties.getProperty(key);
    }
}

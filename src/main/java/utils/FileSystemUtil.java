package utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileSystemUtil {

    private static final File FALLBACK_DB_DIR;
    private static final File DEFAULT_DB_DIR;

    public static File PROPERTIES_PATH;

    static {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if (tmpdir == null) {
            if (SystemUtils.IS_OS_WINDOWS) {
                tmpdir = "c:\\temp";
            } else {
                tmpdir = "/tmp";
            }
        }
        FALLBACK_DB_DIR = new File(tmpdir, ".flowmate");

        String home = System.getProperty("user.home");
        if (home == null) {
            home = System.getProperty("user.dir");
            if (home == null) {
                home = ".";
            }
        }
        DEFAULT_DB_DIR = new File(home, ".flowmate");
    }

    public static Path initDatabaseDirectory() throws IOException {
        String dbDirName = DEFAULT_DB_DIR.getPath();
        File dbDir = new File(dbDirName);

        if (!initDirectory(dbDir)) {
            Logger.getInstance().logToOutput(String.format("Could not create neo4j directory: %s. Using tmp folder: %s%n",
                    dbDir.getAbsolutePath(), FALLBACK_DB_DIR.getAbsolutePath()));

            dbDir = FALLBACK_DB_DIR;
        }

        if (!initDirectory(dbDir)) {
            throw new IOException("Could not create neo4j directory nor temporary directory.");
        }

        File propertiesPath = new File(dbDir, "properties");
        File dbPath = new File(dbDir, "neo");

        PROPERTIES_PATH = propertiesPath.getAbsoluteFile();
        return Path.of(dbPath.getAbsolutePath());
    }

    private static boolean initDirectory(File cooperatorDir) {
        return (cooperatorDir.exists() || cooperatorDir.mkdirs()) && cooperatorDir.canRead() &&
                cooperatorDir.canWrite();
    }
}

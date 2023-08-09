package burp;

import burp.api.montoya.MontoyaApi;
import utils.FileSystemUtil;
import utils.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class PropertiesHandler {

    private MontoyaApi api;

    public PropertiesHandler(MontoyaApi api) {
        this.api = api;
    }

    /*
    *   Checks if the Burp State is matching with the DB by checking if the stored key in the burp state equals
    *   the stored key in the properties file in the db directory
    */
    public boolean isBurpStateMatchingWithDB() {
        // extensionData().getString("IsRightState"); returns null if the key doesn't exist
        String savedKey = this.api.persistence().extensionData().getString("IsRightState");
        File propertiesFile = FileSystemUtil.PROPERTIES_PATH;
        long randomLong = new Random().nextLong();
        String hashed = String.valueOf(Objects.hash(randomLong));
        // If the proxy history is empty, it can be assumed that it's a new project, therefore save key in burp state
        // and properties file
        if (this.api.proxy().history().isEmpty() && !propertiesFile.exists()) {
            this.api.persistence().extensionData().setString("IsRightState", hashed);
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + hashed);
                Logger.getInstance().logToOutput("Saved new Hash in Burp State + " + propertiesFile.toPath());
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (savedKey != null && !propertiesFile.exists()) {
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + savedKey);
                Logger.getInstance().logToOutput(String.format("Database is empty but Burp State is not. Saved Burp State Hash in %s\n To ensure proper working of the extension Create a new Burp Project", propertiesFile.toPath()));
                return false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if (savedKey == null && !propertiesFile.exists()) {
            this.api.persistence().extensionData().setString("IsRightState", hashed);
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + hashed);
                Logger.getInstance().logToOutput("Saved new Hash in Burp State + " + propertiesFile.toPath());
                return true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                var path = propertiesFile.toPath();
                List<String> prop = Files.readAllLines(path);
                Logger.getInstance().logToOutput("Saved in Properties file: " + path);
                String savedHash = this.api.persistence().extensionData().getString("IsRightState");
                String cutHash = prop.get(0).split(":")[1];
                return cutHash.equals(savedHash);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Used for the checkbox in the getting started tab
    // the plugin should remember if the scope checkbox is selected, otherwise the detectionActivated checkbox is
    // not selected
    public boolean isScopeSet() {
        String isSet = this.api.persistence().extensionData().getString("isScopeSet");
        return isSet != null;
    }

    public void setScopeKeyInBurpState(String scope) {
        this.api.persistence().extensionData().setString("isScopeSet", scope);
    }
}

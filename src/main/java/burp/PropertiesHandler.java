package burp;

import audit.*;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.core.ByteArray;
import burp.api.montoya.persistence.PersistedList;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gui.container.RuleContainer;
import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import org.apache.commons.lang3.SerializationUtils;
import utils.FileSystemUtil;
import utils.Logger;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

public class PropertiesHandler {

    private MontoyaApi api;

    public boolean isFirstTimeLoading;
    public boolean isMatching;

    public PropertiesHandler(MontoyaApi api) {
        this.api = api;
        this.isFirstTimeLoading = false;
        this.isMatching = false;
        checkBurpStateMatchingWithDB();
    }

    /*
    *   Checks if the Burp State is matching with the DB by checking if the stored key in the burp state equals
    *   the stored key in the properties file in the db directory
    */
    public void checkBurpStateMatchingWithDB() {
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
                this.isFirstTimeLoading = true;
                this.isMatching = true;
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                throw new RuntimeException(e);
            }
        }
        if (savedKey != null && !propertiesFile.exists()) {
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + savedKey);
                Logger.getInstance().logToOutput(String.format("Database is empty but Burp State is not. Saved Burp State Hash in %s\n To ensure proper working of the extension Create a new Burp Project", propertiesFile.toPath()));
                this.isMatching = false;
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                throw new RuntimeException(e);
            }
        }
        if (savedKey == null && !propertiesFile.exists()) {
            this.api.persistence().extensionData().setString("IsRightState", hashed);
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + hashed);
                Logger.getInstance().logToOutput("Saved new Hash in Burp State + " + propertiesFile.toPath());
                this.isFirstTimeLoading = true;
                this.isMatching = true;
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                throw new RuntimeException(e);
            }
        } else {
            try {
                var path = propertiesFile.toPath();
                List<String> prop = Files.readAllLines(path);
                Logger.getInstance().logToOutput("Saved in Properties file: " + path);
                String savedHash = this.api.persistence().extensionData().getString("IsRightState");
                String cutHash = prop.get(0).split(":")[1];
                this.isMatching = cutHash.equals(savedHash);
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
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

    /*
    * Rules are being saved as:
    * Key: hash of name, regex, affectsParameterNames, affectsParameterValues, affectsHeader, affectsBody, affectsCookie, active, caseSensitive
    * Value: RuleContainer as List<String>
    */
    public void saveNoiseReductionRule(RuleContainer ruleContainer) {
        // List order of Elements is name, regex, affectsParameterNames, affectsParameterValues,
        // affectsHeader, affectsBody, affectsCookie, active, caseSensitive, hash
        PersistedList<String> ruleAsList = ruleContainer.toPersistedList();
        this.api.persistence().extensionData().setStringList(ruleContainer.getHash(), ruleAsList);
    }

    public void updateNoiseReductionRule(RuleContainer ruleContainer, String oldRuleHash) {
        PersistedList<String> newRuleAsList = ruleContainer.toPersistedList();
        this.api.persistence().extensionData().deleteStringList(oldRuleHash);
        this.api.persistence().extensionData().setStringList(ruleContainer.getHash(), newRuleAsList);
    }

    public void deleteNoiseReductionRule(RuleContainer ruleContainer) {
        this.api.persistence().extensionData().deleteStringList(ruleContainer.getHash());
    }

    public List<RuleContainer> loadNoiseReductionRules() {
        Set<String> keys = this.api.persistence().extensionData().stringListKeys();
        List<RuleContainer> rules = new ArrayList<>();
        for (String key : keys) {
            List<String> rule = this.api.persistence().extensionData().getStringList(key);
            // List order of Elements is name, regex, affectsParameterNames, affectsParameterValues,
            // affectsHeader, affectsBody, affectsCookie, active, caseSensitive, hash (10 Elements)
            String name = rule.get(0);
            String regex = rule.get(1);
            boolean affectsParameterNames = Boolean.parseBoolean(rule.get(2));
            boolean affectsParameterValues = Boolean.parseBoolean(rule.get(3));
            boolean affectsHeader = Boolean.parseBoolean(rule.get(4));
            boolean affectsBody = Boolean.parseBoolean(rule.get(5));
            boolean affectsCookie = Boolean.parseBoolean(rule.get(6));
            boolean active = Boolean.parseBoolean(rule.get(7));
            boolean caseInsensitive = Boolean.parseBoolean(rule.get(8));
            RuleContainer ruleContainer = new RuleContainer(name, regex, affectsParameterNames,
                    affectsParameterValues, affectsHeader, affectsBody, affectsCookie, active, caseInsensitive);
            rules.add(ruleContainer);
        }
        return rules;
    }

    public void setDefaultRulesOnFirstLoad() {
        if (isFirstTimeLoading && loadNoiseReductionRules().isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String filepath = FileSystemUtil.DEFAULT_RULES_PATH;
                URL fileUrl = this.getClass().getResource(filepath);
                List<RuleContainer> ruleContainers = objectMapper.readValue(fileUrl, new TypeReference<List<RuleContainer>>() {
                });
                for (RuleContainer rule : ruleContainers) {
                    saveNoiseReductionRule(rule);
                }
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
        }
    }
    public void restoreDefaultNoiseReductionRules() {
        this.isFirstTimeLoading = true;
        Set<String> keys = this.api.persistence().extensionData().stringListKeys();
        for (String key : keys) {
            this.api.persistence().extensionData().deleteStringList(key);
        }
        setDefaultRulesOnFirstLoad();
        this.isFirstTimeLoading = false;
    }

    public void makeBurpStateMatchWithDB() {
        if (!isMatching) {
            File propertiesFile = FileSystemUtil.PROPERTIES_PATH;
            long randomLong = new Random().nextLong();
            String hashed = String.valueOf(Objects.hash(randomLong));
            if (propertiesFile.exists()) {
                propertiesFile.delete();
            }
            try {
                propertiesFile.createNewFile();
                Files.writeString(propertiesFile.toPath(), "IsRightState:" + hashed);
                this.api.persistence().extensionData().setString("IsRightState", hashed);
                Logger.getInstance().logToOutput("Saved new Hash in Burp State + " + propertiesFile.toPath());
            } catch (IOException e) {
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
                throw new RuntimeException(e);
            }
            this.isMatching = true;
        }
    }

    public void saveHistoryStartValueInState(int historyStart) {
        this.api.persistence().extensionData().setInteger("historyStart", historyStart);
    }

    public int loadHistoryStartValueInState() {
        var size = this.api.persistence().extensionData().getInteger("historyStart");
        return Objects.requireNonNullElse(size, 0);
    }

    public void saveAuditFinding(AuditFinding finding) {
        byte[] data = SerializationUtils.serialize(finding);
        this.api.persistence().extensionData().setByteArray(finding.getHash(), ByteArray.byteArray(data));
    }

    public List<AuditFinding> loadAuditFindings() {
        Logger.getInstance().logToOutput("Loading Audit Findings...");
        Set<String> keys = this.api.persistence().extensionData().byteArrayKeys();
        List<AuditFinding> findings = new ArrayList<>();
        for (String key : keys) {
            ByteArray auditFindingAsByteArray = this.api.persistence().extensionData().getByteArray(key);
            try {
                org.apache.commons.io.serialization.ValidatingObjectInputStream in =
                        new ValidatingObjectInputStream(new ByteArrayInputStream(auditFindingAsByteArray.getBytes()));
                in.accept(java.lang.Enum.class, AuditFinding.class, AuditFinding.FindingSeverity.class, CrossContentTypeAuditFinding.class,
                        CrossScopeAuditFinding.class, CrossSessionAuditFinding.class, HeaderMatchAuditFinding.class, KeywordMatchAuditFinding.class,
                        LongDistanceMatchAuditFinding.class);
                AuditFinding auditFinding = (AuditFinding) in.readObject();
                in.close();
                findings.add(auditFinding);
            } catch (IOException | ClassNotFoundException e) {
                Logger.getInstance().logToOutput("Audit Finding could not be loaded! View Error log for more information");
                Logger.getInstance().logToError(Arrays.toString(e.getStackTrace()));
            }
        }
        Logger.getInstance().logToOutput("Loading Audit Findings finished");
        return findings;
    }

    public void deleteAuditFindings() {
        Set<String> keys = this.api.persistence().extensionData().byteArrayKeys();
        for (String key : keys) {
            this.api.persistence().extensionData().deleteByteArray(key);
        }
    }

}

package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.Relationship;
import utils.Logger;
import utils.PatternEscape;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class InputParameter {
    @Id
    private int identifier;
    private String name;
    private String type;
    private String domain;

    private boolean excludedByNoiseReduction;

    @Relationship(type = "OCCURS_WITH_VALUE", direction = Relationship.Direction.OUTGOING)
    private List<InputValue> occurrenceEntities = new ArrayList<>();


    // Empty Constructor needed for neo4J
    public InputParameter() {

    }

    public InputParameter(String name, String type, String domain) {
        this.name = name;
        this.type = type;
        this.domain = domain;
        this.identifier = Objects.hash(name, type, domain);
        this.excludedByNoiseReduction = false;
    }

    public String getName() {
        return name;
    }

    public int getIdentifier(){
        return this.identifier;
    }

    public String getDomain() {
        return domain;
    }

    public String getType() {
        return this.type;
    }

    public void addOccurence(InputValue occurrence) {
        this.occurrenceEntities.add(occurrence);
    }

    public List<InputValue> getOccurrenceEntities() {
        return occurrenceEntities;
    }

    public boolean isExcludedByNoiseReduction() {
        return excludedByNoiseReduction;
    }

    public void setExcludedByNoiseReduction(boolean excludedByNoiseReduction) {
        this.excludedByNoiseReduction = excludedByNoiseReduction;
    }

    public Pattern getRegexMatchingValueByIdentifier(int identifier) {
        var occurrence = this.getOccurrenceByIdentifier(identifier);
        if(occurrence == null){
            Logger.getInstance().logToOutput("[InputParameter] The occurrence with the sequence number specified doesn't exist");
            return null;
        }

        var value = occurrence.getValue();

        var escaped = PatternEscape.escapeForRegex(value);
        var regex = String.format("[><\\\"\\\'\\)\\(\\{\\}\\s\\=\\:]%s[><\\\"\\\'\\)\\(\\{\\}\\s]", escaped);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }

    public InputValue getOccurrenceByIdentifier(int identifier) {
        var occurrence = this.occurrenceEntities.stream().filter(e -> e.getIdentifier() == identifier).findAny();

        try {
            return occurrence.get();
        } catch (Exception ex) {
            return null;
        }
    }

    public Pattern getRegexForHeaderMatchingValueByIdentifier(int identifier) {
        var occurrence = this.getOccurrenceByIdentifier(identifier);
        if(occurrence == null){
            Logger.getInstance().logToOutput("[InputParameter] The occurrence with the sequence number specified doesn't exist");
            return null;
        }

        var value = occurrence.getValue();

        var escaped = PatternEscape.escapeForRegex(value);
        var regex = String.format("(?i)([^\\s:]+):\\s+(.*%s.*)", escaped);
        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
    }


    @Override
    public String toString() {
        return "InputParameter{" +
                "\nidentifier=\n'" + identifier + '\'' +
                "\nname='" + name + '\'' +
                "\n type='" + type + '\'' +
                "\n domain='" + domain + '\'' +
                "\n occurrenceEntities=" + occurrenceEntities +
                '}';
    }
}

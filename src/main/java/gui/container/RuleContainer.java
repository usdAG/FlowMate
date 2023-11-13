package gui.container;

import burp.api.montoya.persistence.PersistedList;
import org.apache.commons.text.StringEscapeUtils;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class RuleContainer {

    private String name;
    private String parametersAffected;
    private String typesAffected;
    private String regex;
    private String activeSymbol;
    private boolean affectsParameterNames;
    private boolean affectsParameterValues;
    private boolean affectsQueryString;
    private boolean affectsBody;
    private boolean affectsCookie;
    private boolean active;
    private boolean caseInsensitive;

    public RuleContainer() {
        createStrings(this.affectsParameterNames, this.affectsParameterValues, this.affectsQueryString, this.affectsBody, this.affectsCookie, this.active);
    }

    public RuleContainer(String name, String regex, boolean affectsParameterNames, boolean affectsParameterValues,
                         boolean affectsQueryString, boolean affectsBody, boolean affectsCookie, boolean active, boolean caseInsensitive) {
        this.name = name;
        this.regex = regex;
        this.affectsParameterNames = affectsParameterNames;
        this.affectsParameterValues = affectsParameterValues;
        this.affectsQueryString = affectsQueryString;
        this.affectsBody = affectsBody;
        this.affectsCookie = affectsCookie;
        this.active = active;
        this.caseInsensitive = caseInsensitive;
        createStrings(affectsParameterNames, affectsParameterValues, affectsQueryString, affectsBody, affectsCookie, active);
    }

    public void createStrings(boolean affectsParameterNames, boolean affectsParameterValues,
                               boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean isActive) {

       StringJoiner params = new StringJoiner("/");
       StringJoiner types = new StringJoiner("/");

       params.setEmptyValue("");
       types.setEmptyValue("");

       if (affectsParameterNames)
           params.add("Names");
       if (affectsParameterValues)
           params.add("Values");
       if (affectsHeader)
           types.add("Header");
       if (affectsBody)
           types.add("Body");
       if (affectsCookie)
           types.add("Cookie");

       this.parametersAffected = params.toString();
       this.typesAffected = types.toString();
       if (isActive) {
           this.activeSymbol = "\u25B6";
       } else {
           this.activeSymbol = "\u25A0";
       }
    }

    public String getLabelRepresentation() {
        String content = """
            <html>
            <b>RULE_NAME</b> <br>
            <br>
            Affects Parameter: <b>AFFECTED_PARAMETER</b> <br>
            Affects Type: <b>AFFECTED_TYPES</b> <br>
            <br>
            Regex: <b>REGEX</b>
            <div float:right style="font-size:12px;">ACTIVE_SYMBOL</div>
            </html>
            """;
        return content.replace("RULE_NAME", escapeHtmlEmbeddedStrings(this.name))
                .replace("AFFECTED_PARAMETER", escapeHtmlEmbeddedStrings(this.parametersAffected))
                .replace("AFFECTED_TYPES", escapeHtmlEmbeddedStrings(this.typesAffected))
                .replace("REGEX", escapeHtmlEmbeddedStrings(this.regex))
                .replace("ACTIVE_SYMBOL", this.activeSymbol);
    }

    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

    public void updateValues(String name, String regex, boolean affectsParameterNames, boolean affectsParameterValues,
                             boolean affectsHeader, boolean affectsBody, boolean affectsCookie, boolean active, boolean caseSensitive) {
        this.setName(name);
        this.setRegex(regex);
        this.setAffectsParameterNames(affectsParameterNames);
        this.setAffectsParameterValues(affectsParameterValues);
        this.setAffectsQueryString(affectsHeader);
        this.setAffectsBody(affectsBody);
        this.setAffectsCookie(affectsCookie);
        this.setActive(active);
        this.setCaseInsensitive(caseSensitive);
        createStrings(affectsParameterNames, affectsParameterValues, affectsHeader, affectsBody, affectsCookie, active);
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.createStrings(this.affectsParameterNames, this.affectsParameterValues, this.affectsQueryString, this.affectsBody, this.affectsCookie, this.active);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public boolean affectsParameterNames() {
        return affectsParameterNames;
    }

    public void setAffectsParameterNames(boolean affectsParameterNames) {
        this.affectsParameterNames = affectsParameterNames;
    }

    public boolean affectsParameterValues() {
        return affectsParameterValues;
    }

    public void setAffectsParameterValues(boolean affectsParameterValues) {
        this.affectsParameterValues = affectsParameterValues;
    }

    public boolean affectsQueryString() {
        return affectsQueryString;
    }

    public void setAffectsQueryString(boolean affectsQueryString) {
        this.affectsQueryString = affectsQueryString;
    }

    public boolean affectsBody() {
        return affectsBody;
    }

    public void setAffectsBody(boolean affectsBody) {
        this.affectsBody = affectsBody;
    }

    public boolean affectsCookie() {
        return affectsCookie;
    }

    public void setAffectsCookie(boolean affectsCookie) {
        this.affectsCookie = affectsCookie;
    }

    public boolean isCaseInsensitive() {
        return caseInsensitive;
    }

    public void setCaseInsensitive(boolean caseInsensitive) {
        this.caseInsensitive = caseInsensitive;
    }

    public PersistedList<String> toPersistedList() {
        List<String> list = List.of(name,
                regex,
                String.valueOf(affectsParameterNames),
                String.valueOf(affectsParameterValues),
                String.valueOf(affectsQueryString),
                String.valueOf(affectsBody),
                String.valueOf(affectsCookie),
                String.valueOf(active),
                String.valueOf(caseInsensitive),
                getHash());

        PersistedList<String> persistedList = PersistedList.persistedStringList();
        persistedList.addAll(list);
        return persistedList;
    }

    public String getHash() {
        return String.valueOf(Objects.hash(name, regex, affectsParameterNames, affectsParameterValues, affectsQueryString, affectsBody, affectsCookie, active, caseInsensitive));
    }
}

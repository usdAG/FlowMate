package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class SessionParameterContainer {

    private String name;
    private String value;
    private String type;
    private int matches;
    private String occurredIn;
    private String url;

    public SessionParameterContainer(String name, String value, String type, int matches, String occurredIn, String url){
        this.name = name;
        this.value = value;
        this.type = type;
        this.matches = matches;
        this.occurredIn = occurredIn;
        this.url = url;
    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>Name: </b>%s <br><b>Value: </b>%s<br><b>Type: </b>%s <b>Matches: </b> %s <br><b>Entered in: </b> %s",
                escapeHtmlEmbeddedStrings(this.name), escapeHtmlEmbeddedStrings(this.value), escapeHtmlEmbeddedStrings(this.type), this.matches, escapeHtmlEmbeddedStrings(this.occurredIn));
        return content;
    }

    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public String getUrl() {
        return url;
    }

    public int getMatches() {
        return matches;
    }
}

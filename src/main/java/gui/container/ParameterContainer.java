package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class ParameterContainer {

    private String name;
    private String type;
    private int occurrences;
    private int matches;

    public ParameterContainer(String name, String type, int occurrences, int matches){
        this.name = name;
        this.type = type;
        this.occurrences = occurrences;
        this.matches = matches;

    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>Name: </b>%s<br><b>Type: </b>%s <b>Occurrences: </b>%s <b>Matches: </b> %s",
                escapeHtmlEmbeddedStrings(this.name), escapeHtmlEmbeddedStrings(this.type), this.occurrences, this.matches);
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

    public int getOccurrences() {
        return occurrences;
    }

    public int getMatches() {
        return matches;
    }
}

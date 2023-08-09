package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class SessionParamMonitorContainer {

    private String name;
    private String value;
    private String changedAt;
    private String changedTo;

    public SessionParamMonitorContainer(String name, String value, String changedAt, String changedTo){
        this.name = name;
        this.value = value;
        this.changedAt = changedAt;
        this.changedTo = changedTo;
    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>Name: </b>%s<br><b>Value: </b>%s<br><b>Changed to: </b>%s",
                escapeHtmlEmbeddedStrings(this.name), escapeHtmlEmbeddedStrings(this.value),
                escapeHtmlEmbeddedStrings(this.changedTo));
        return content;
    }
    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}

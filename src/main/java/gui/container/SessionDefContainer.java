package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class SessionDefContainer {

    private String name;
    private String type;

    public SessionDefContainer(String name, String type){
        this.name = name;
        this.type = type;
    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>Name: </b>%s<br><b>Type: </b>%s",
                escapeHtmlEmbeddedStrings(this.name), escapeHtmlEmbeddedStrings(this.type));
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
}

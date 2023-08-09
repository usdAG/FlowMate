package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class InputParameterContainer {

    private String url;
    private String value;

    private String messageHash;
    private int messageId;

    public InputParameterContainer(String url, String value, String messageHash, int messageId){
        this.url = url;
        this.value = value;
        this.messageHash = messageHash;
        this.messageId = messageId;
    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>URL: </b>%s<br><b>Value: </b>%s",
                escapeHtmlEmbeddedStrings(this.url), escapeHtmlEmbeddedStrings(this.value));
        return content;
    }

    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

    public String getUrl() {
        return url;
    }

    public String getValue() {
        return value;
    }

    public String getMessageHash() {
        return messageHash;
    }

    public int getMessageId() {
        return messageId;
    }
}

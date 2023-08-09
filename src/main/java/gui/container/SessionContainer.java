package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class SessionContainer {

    private String name;
    private String createdAt;
    private int lowId;
    private int highId;
    private String range;

    public SessionContainer(String name, String CreatedAt, int lowId, int highId){
        this.name = name;
        this.createdAt = CreatedAt;
        this.lowId = lowId;
        this.highId = highId;
        this.range = lowId + " - " + highId;
    }

    public String getLabelRepresentation(){
        String content = "";
        content = String.format("<html><b>Name: </b>%s<br><b>Created At: </b>%s <b>Id Range: </b>%s",
                escapeHtmlEmbeddedStrings(this.name), escapeHtmlEmbeddedStrings(this.createdAt), escapeHtmlEmbeddedStrings(this.range));
        return content;
    }
    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

    public String getName() {
        return name;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getLowId() {
        return lowId;
    }

    public int getHighId() {
        return highId;
    }

    public void updateRange(int highId) {
        this.highId = highId;
        this.range = lowId + " - " + highId;
    }


}

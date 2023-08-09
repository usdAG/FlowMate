package gui.container;

import org.apache.commons.text.StringEscapeUtils;

public class MatchValueContainer {

    private String responseConentType;
    private String matchProof;

    public MatchValueContainer(String responseConentType, String matchProof){
        this.responseConentType = responseConentType;
        this.matchProof = matchProof;
    }

    public String getLabelRepresentation(){
        String content = "";
        String values = "";
        values += String.format("%s<br>", escapeHtmlEmbeddedStrings(matchProof));
        content = String.format("<html><b>Content-Type: </b>%s<br><b>ParameterMatch Proof:</b><br>%s",
                escapeHtmlEmbeddedStrings(this.responseConentType), values);
        return content;
    }

    private String escapeHtmlEmbeddedStrings(String input){
        return StringEscapeUtils.escapeHtml4(input);
    }

}

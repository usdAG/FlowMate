package db;

import burp.HttpResponse;
import db.entities.InputParameter;
import db.entities.InputValue;

public class MatchHelperClass {

    HttpResponse response;
    private String name;
    private String value;
    private String type;
    private String responseContentType;
    private String matchProof;
    private String url;
    private String messageHash;
    private InputParameter inputParameter;
    private InputValue inputValue;


    // Helper Class to store all relevant properties to create and save the entities in MatchHandler class
    public MatchHelperClass(HttpResponse response, String name, String value, String type, String responseContentType, String matchProof, String url, String messageHash, InputParameter inputParameter, InputValue inputValue) {
        this.response = response;
        this.name = name;
        this.value = value;
        this.type = type;
        this.responseContentType = responseContentType;
        this.matchProof = matchProof;
        this.url = url;
        this.messageHash = messageHash;
        this.inputParameter = inputParameter;
        this.inputValue = inputValue;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public String getMatchProof() {
        return matchProof;
    }

    public String getUrl() {
        return url;
    }

    public String getMessageHash() {
        return this.messageHash;
    }

    public InputParameter getInputParameterObj(){
        return this.inputParameter;
    }

    public InputValue getInputValueObj(){
        return this.inputValue;
    }
}

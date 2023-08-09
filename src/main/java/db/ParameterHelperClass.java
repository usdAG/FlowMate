package db;

import burp.ParameterType;

public class ParameterHelperClass {

    private String name;
    private ParameterType type;
    private String domain;
    private String urlFound;
    private String value;

    public ParameterHelperClass(String name, ParameterType type, String domain, String urlFound, String value) {
        this.name = name;
        this.type = type;
        this.domain = domain;
        this.urlFound = urlFound;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public ParameterType getType() {
        return type;
    }

    public String getDomain() {
        return domain;
    }

    public String getUrlFound() {
        return urlFound;
    }

    public String getValue() {
        return value;
    }
}

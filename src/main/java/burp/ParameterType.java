package burp;

public enum ParameterType {
    URL,
    BODY,
    COOKIE,
    JSON,
    OTHER;

    public String getName(){
        switch(this){
            case URL:
                return "GET";
            case BODY:
                return "POST";
            case COOKIE:
                return "COOKIE";
            case JSON:
                return "JSON";
            default:
                return "OTHER";
        }
    }

}
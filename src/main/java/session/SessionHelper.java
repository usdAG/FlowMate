package session;

public class SessionHelper {

    private String paramName;
    private String paramValue;
    private int lowestId;
    private int highestId;

    public SessionHelper(String paramName, String paramValue, int lowestMessageId, int highestMessageId) {
        this.paramName = paramName;
        this.paramValue = paramValue;
        this.lowestId = lowestMessageId;
        this.highestId = highestMessageId;
    }

    public String getParamName() {
        return paramName;
    }

    public String getParamValue() {
        return paramValue;
    }

    public int getLowestId() {
        return lowestId;
    }

    public int getHighestId() {
        return highestId;
    }

    @Override
    public String toString() {
        return "SessionHelper{" + paramName + '\'' + ",'" + paramValue + '\'' + "," + lowestId + "," + highestId + '}';
    }
}

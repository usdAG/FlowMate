package session;

import java.util.List;

public class Session {

    private String name;
    private int lowestHistoryId;
    private int highestHistoryId;
    private List<SessionParameter> sessionParameter;
    private boolean isUnauthenticatedSession = false;

    public Session(String name, int lowestHistoryId, int highestHistoryId, List<SessionParameter> sessionParameter) {
        this.name = name;
        this.lowestHistoryId = lowestHistoryId;
        this.highestHistoryId = highestHistoryId;
        this.sessionParameter = sessionParameter;
    }

    public Session(String name, int lowestHistoryId, int highestHistoryId, List<SessionParameter> sessionParameter, boolean isUnauthenticatedSession) {
        this.name = name;
        this.lowestHistoryId = lowestHistoryId;
        this.highestHistoryId = highestHistoryId;
        this.sessionParameter = sessionParameter;
        this.isUnauthenticatedSession = true;
    }

    public List<SessionParameter> getSessionParameter() {
        return sessionParameter;
    }

    public String getName() {
        return name;
    }

    public int getLowestHistoryId() {
        return lowestHistoryId;
    }

    public boolean isUnauthenticatedSession() {
        return isUnauthenticatedSession;
    }

    public void setSessionParameter(List<SessionParameter> sessionParameter) {
        this.sessionParameter = sessionParameter;
    }
    @Override
    public String toString() {
        return "Session{" +
                "name='" + name + '\'' +
                ", lowestHistoryId=" + lowestHistoryId +
                ", highestHistoryId=" + highestHistoryId +
                ", sessionParameter=" + sessionParameter +
                '}';
    }
}

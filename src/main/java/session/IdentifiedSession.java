package session;

import java.util.HashMap;
import java.util.Map;

public class IdentifiedSession {

    private int idStart;
    private int idEnd;
    public Map<String, SessionHelper> sessionHelperMap = new HashMap<>();

    public IdentifiedSession(int idStart, int idEnd) {
        this.idStart = idStart;
        this.idEnd = idEnd;
    }

    public boolean inRange(SessionHelper sessionHelper) {
        return sessionHelper.getLowestId() <= idEnd && sessionHelper.getHighestId() >= idStart;
    }

    public boolean contains(SessionHelper sessionHelper) {
        SessionHelper existingHelper = sessionHelperMap.get(sessionHelper.getParamName());
        return existingHelper != null && !existingHelper.getParamValue().equals(sessionHelper.getParamValue());
    }

    public void updateRange(SessionHelper sessionHelper) {
        idStart = Math.min(idStart, sessionHelper.getLowestId());
        idEnd = Math.max(idEnd, sessionHelper.getHighestId());
    }
}

package db.entities;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@NodeEntity
public class Session {

    @Id
    private int identifier;
    private String name;
    private int lowestHistoryId;
    private int highestHistoryId;
    @Relationship(type="CONTAINS_PARAMETER",direction = Relationship.Direction.UNDIRECTED)
    private List<SessionParameter> sessionParameter = new ArrayList<>();
    @Relationship(type = "INPUT_VALUE_FOUND_IN_SESSION", direction = Relationship.Direction.UNDIRECTED)
    private List<InputValue> inputValuesRelatedToSession = new ArrayList<>();
    @Relationship(type = "PARAMETER_MATCH_FOUND_IN_SESSION", direction = Relationship.Direction.UNDIRECTED)
    private List<ParameterMatch> parameterMatchesRelatedToSession = new ArrayList<>();
    @Relationship(type = "MATCH_VALUE_FOUND_IN_SESSION", direction = Relationship.Direction.UNDIRECTED)
    private List<MatchValue> matchValuesRelatedToSession = new ArrayList<>();
    private boolean isUnauthenticatedSession = false;

    public Session() {}

    public Session(String name, int lowestHistoryId, int highestHistoryId, List<SessionParameter> sessionParameter) {
        this.name = name;
        this.lowestHistoryId = lowestHistoryId;
        this.highestHistoryId = highestHistoryId;
        this.sessionParameter = sessionParameter;
        this.identifier = Objects.hash(this.lowestHistoryId, this.highestHistoryId);
    }

    public Session(String name, int lowestHistoryId, int highestHistoryId, List<SessionParameter> sessionParameter, boolean isUnauthenticatedSession) {
        this.name = name;
        this.lowestHistoryId = lowestHistoryId;
        this.highestHistoryId = highestHistoryId;
        this.sessionParameter = sessionParameter;
        this.isUnauthenticatedSession = true;
        this.identifier = Objects.hash(this.lowestHistoryId, this.highestHistoryId);
    }

    public List<SessionParameter> getSessionParameter() {
        return sessionParameter;
    }

    public void addMatch(ParameterMatch match) {
        this.parameterMatchesRelatedToSession.add(match);
    }

    public void addMatchValue(MatchValue value) {
        this.matchValuesRelatedToSession.add(value);
    }

    public void addInputValue(InputValue inputValue) {
        this.inputValuesRelatedToSession.add(inputValue);
    }

    public String getName() {
        return name;
    }

    public int getLowestHistoryId() {
        return lowestHistoryId;
    }

    public int getHighestHistoryId() {
        return highestHistoryId;
    }

    public boolean isUnauthenticatedSession() {
        return isUnauthenticatedSession;
    }

    public int getIdentifier() {
        return identifier;
    }

    public List<InputValue> getInputValuesRelatedToSession() {
        return inputValuesRelatedToSession;
    }

    public void setInputValuesRelatedToSession(List<InputValue> inputValuesRelatedToSession) {
        this.inputValuesRelatedToSession = inputValuesRelatedToSession;
    }

    public void setParameterMatchesRelatedToSession(List<ParameterMatch> parameterMatchesRelatedToSession) {
        this.parameterMatchesRelatedToSession = parameterMatchesRelatedToSession;
    }

    public void setMatchValuesRelatedToSession(List<MatchValue> matchValuesRelatedToSession) {
        this.matchValuesRelatedToSession = matchValuesRelatedToSession;
    }

    public void setSessionParameter(List<SessionParameter> sessionParameter) {
        this.sessionParameter = sessionParameter;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setHighestHistoryId(int highestHistoryId) {
        this.highestHistoryId = highestHistoryId;
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

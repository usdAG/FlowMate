package burp;

import burp.api.montoya.MontoyaApi;
import db.CypherQueryHandler;
import db.DBModel;
import db.MatchHandler;
import db.entities.InputValue;
import db.entities.MatchValue;
import db.entities.InputParameter;
import db.entities.ParameterMatch;
import gui.container.*;
import org.neo4j.ogm.model.Result;
import utils.Hashing;

import java.util.*;

// Used to convert List into Container Vectors which are rendered in the JLists
public class ContainerConverter {

    private MontoyaApi api;
    private MatchHandler matchHandler;

    public ContainerConverter(MontoyaApi api, MatchHandler matchHandler) {
        this.api = api;
        this.matchHandler = matchHandler;
    }

    public Vector<ParameterContainer> parameterToContainer(List<InputParameter> inputParameters) {
        var list = new Vector<ParameterContainer>();
        var parameterList = inputParameters.stream().distinct().sorted(Comparator.comparing(InputParameter::getName)).toList();
        List<Integer> duplicates = new ArrayList<>();
        for (var parameter : parameterList) {
            int identifier = parameter.getIdentifier();
            if (!duplicates.contains(identifier)) {
                duplicates.add(identifier);
                var name = parameter.getName();
                var type = parameter.getType();
                Map<String, String> values = Collections.singletonMap("name", name);
                String query = "MATCH (n:InputParameter {name: $name, type: \"%s\"})-[OCCURS_WITH_VALUE]-(o:InputValue) RETURN o".formatted(type);
                Result result = DBModel.query(query, values);
                List<InputValue> inputValueList = new ArrayList<>(CypherQueryHandler.getOccurrencesFromQueryResult(result));
                int occurrences = inputValueList.size();
                int matches = getNumberOfMatchesForParameterName(name, type);
                list.add(new ParameterContainer(name, type, occurrences, matches));
            }
        }
        return list;
    }
    // Find all Parameters that belong to a Session
    public Vector<SessionParameterContainer> parameterToContainerSessionDef(List<ParameterMatch> matchesList, String sessionName, List<String> sessionDefParameterNames) {
        var list = new HashSet<SessionParameterContainer>();
        Set<ParameterMatch> parameterMatches = new HashSet<>(matchesList);
        var duplicates = new ArrayList<>();

        for (ParameterMatch match : parameterMatches) {
            Map<String, String> values = Map.of("name", match.getName(), "type", match.getType(), "value", match.getValue());
            String valueQuery = "MATCH (p:InputParameter {name: $name, type: $type})-[OCCURS_WITH_VALUE]-(m:InputValue {type: $type, value: $value}) RETURN m";
            Result result = DBModel.query(valueQuery, values);
            for (var parameterValue : CypherQueryHandler.getOccurrencesFromQueryResult(result).stream().distinct().toList()) {
                String type = parameterValue.getType();
                String value = parameterValue.getValue();
                String occurredIn = parameterValue.getSession();
                String name = match.getName();
                String url = parameterValue.getUrl();
                // Do not show Parameters defined as Session
                if (sessionDefParameterNames.stream().anyMatch(s -> s.equals(name)))
                    continue;
                int matches = getNumberOfMatchesForSessionTabForParameterName(name, value, type, sessionName);
                if (matches != 0) {
                    var hash = Objects.hash(name, value, type, matches, occurredIn);
                    if (!duplicates.contains(hash)) {
                        duplicates.add(hash);
                        list.add(new SessionParameterContainer(name, value, type, matches, occurredIn, url));
                    }
                }
            }
        }
        return new Vector<>(list.stream().sorted(Comparator.comparing(SessionParameterContainer::getName)).toList());
    }

    public Vector<InputParameterContainer> parameterOccurrenceToContainer(List<InputValue> occurrences){
        var list = new Vector<InputParameterContainer>();
        List<InputValue> inputValueList = occurrences.stream().distinct().sorted(Comparator.comparing(InputValue::getMessageHash)).toList();
        List<Integer> duplicates2 = new ArrayList<>();
        for (var parameterValue : inputValueList) {
            int identifier = parameterValue.getIdentifier();
            if (!duplicates2.contains(identifier)) {
                duplicates2.add(identifier);
                var url = parameterValue.getUrl();
                var value = parameterValue.getValue();
                var messageHash = parameterValue.getMessageHash();
                list.add(new InputParameterContainer(url, value, messageHash, calculateId(messageHash)));
            }
        }
        return new Vector<>(list.stream().sorted(Comparator.comparing(InputParameterContainer::getMessageId)).toList());
    }

    public Vector<ParameterMatchContainer> matchOccurrenceToContainer(List<ParameterMatch> occurrences){
        var matchEntities = occurrences.stream().distinct().sorted(Comparator.comparing(ParameterMatch::getMessageHash)).toList();
        var duplicates = new ArrayList<>();
        var list = new HashSet<ParameterMatchContainer>();
        for(var occurrence : matchEntities) {
            var hash = Objects.hash(occurrence.getUrl(), occurrence.getValue(), occurrence.getMessageHash(), calculateId(occurrence.getMessageHash()));
            if (!duplicates.contains(hash)) {
                duplicates.add(hash);
                list.add(new ParameterMatchContainer(occurrence.getUrl(), occurrence.getValue(), occurrence.getMessageHash(), calculateId(occurrence.getMessageHash())));
            }
        }
        return new Vector<>(list.stream().sorted(Comparator.comparing(ParameterMatchContainer::getMessageId)).toList());
    }

    public Vector<MatchValueContainer> entryOccurrenceToContainer(List<MatchValue> occurrences){
        var list = new HashSet<MatchValueContainer>();
        for(var occurrence : occurrences)
            list.add(new MatchValueContainer(occurrence.getResponseContentType(), occurrence.getMatchProof()));
        return new Vector<>(list);
    }

    public int getNumberOfMatchesForParameterName(String paramName, String type) {
        List<ParameterMatch> parameterMatchList = new ArrayList<>(this.matchHandler.observableParameterMatchList.stream().toList());
        List<Integer> duplicates = new ArrayList<>();
        List<ParameterMatch> correctParameterMatchList = new ArrayList<>();
        for (var match : parameterMatchList) {
            int identifier = match.getIdentifier();
            if (!duplicates.contains(identifier)) {
                duplicates.add(identifier);
                correctParameterMatchList.add(match);
            }
        }
        return correctParameterMatchList.stream().distinct().filter(e -> e.getName().equals(paramName) && e.getType().equals(type)).toList().size();
    }

    public int getNumberOfMatchesForSessionTabForParameterName(String paramName, String value, String type, String sessionName) {
        Map<String, String> values = Map.of("sessionName", sessionName, "value", value, "type", type);
        String query = "MATCH (m:ParameterMatch {session: $sessionName, value: $value, type: $type}) RETURN m";
        Result result = DBModel.query(query, values);
        List<ParameterMatch> parameterMatchList = new ArrayList<>(CypherQueryHandler.getMatchesFromQueryResult(result));
        return parameterMatchList.stream().distinct().filter(e -> e.getName().equals(paramName)).toList().size();
    }

    private int calculateId(String messageHash) {
        var history = this.api.proxy().history();
        for (int i = 0; i < history.size(); i++) {
            String proxyHash = Hashing.sha1(history.get(i).finalRequest().toByteArray().getBytes());
            if (messageHash.equals(proxyHash)) {
                return i + 1;
            }
        }
        return -1;
    }
}

package db;

import db.entities.InputValue;
import db.entities.MatchValue;
import db.entities.ParameterMatch;
import org.neo4j.ogm.model.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Helper Class that helps to retrieve List Objects from a query result
public class CypherQueryHandler {

    public static List<InputValue> getOccurrencesFromQueryResult(Result queryResult) {
        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
        List<InputValue> occurrences = new ArrayList<>();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            if (result.get("o") != null)
                occurrences.add((InputValue) result.get("o"));
        }
        return occurrences;
    }

    public static List<ParameterMatch> getParameterMatchesFromQueryResult(Result queryResult) {
        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
        List<ParameterMatch> parameterMatches = new ArrayList<>();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            if (result.get("m") != null)
                parameterMatches.add((ParameterMatch) result.get("m"));
        }
        return parameterMatches;
    }

    public static List<MatchValue> getMatchValuesFromQueryResult(Result queryResult) {
        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
        List<MatchValue> matchValues = new ArrayList<>();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            if (result.get("m") != null)
                matchValues.add((MatchValue) result.get("mv"));
        }
        return matchValues;
    }

}

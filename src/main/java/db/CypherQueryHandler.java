package db;

import db.entities.InputValue;
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
            occurrences.add((InputValue) result.get("m"));
        }
        return occurrences;
    }

    public static List<ParameterMatch> getMatchesFromQueryResult(Result queryResult) {
        Iterator<Map<String, Object>> resultIterator = queryResult.queryResults().iterator();
        List<ParameterMatch> occurrences = new ArrayList<>();
        while (resultIterator.hasNext()) {
            Map<?, ?> result = resultIterator.next();
            occurrences.add((ParameterMatch) result.get("m"));
        }
        return occurrences;
    }
}

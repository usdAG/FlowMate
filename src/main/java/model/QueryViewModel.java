package model;
import db.DBModel;
import db.entities.InputValue;
import db.entities.MatchValue;
import db.entities.ParameterMatch;
import org.neo4j.ogm.model.Result;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class QueryViewModel {

    private String selectedText = "Name";

    public List<MatchValue> matchValueEntityList;

    public List<InputValue> occurrenceEntityList;
    public List<ParameterMatch> parameterMatchEntityList;

    public QueryViewModel() {
        this.matchValueEntityList = new ArrayList<>();
        this.occurrenceEntityList = new ArrayList<>();
        this.parameterMatchEntityList = new ArrayList<>();
    }

    public String getSelectedText() {
        return selectedText;
    }

    public void setSelectedText(String selectedText) {
        this.selectedText = selectedText;
    }

    public void loadListData(String paramName, String paramType) {
        this.matchValueEntityList.clear();
        this.occurrenceEntityList.clear();
        this.parameterMatchEntityList.clear();
        String query = "MATCH (n:InputParameter {name: $param, type: \"%s\"})-[OCCURS_WITH_VALUE]->(o:InputValue)".formatted(paramType) +
                "OPTIONAL MATCH (m:ParameterMatch {name: $param, type: \"%s\"})-[MATCH]->(e:MatchValue)".formatted(paramType) +
                "RETURN o, m, e";
        Map<String, String> params = Map.of("param", paramName);
        Result queryResult = DBModel.query(query, params);
        Iterator<Map<String, Object>> results = queryResult.queryResults().iterator();

        while (results.hasNext()) {
            Map<?, ?> result = results.next();
            occurrenceEntityList.add((InputValue) result.get("o"));
            if (result.get("m") != null) {
                parameterMatchEntityList.add((ParameterMatch) result.get("m"));
                matchValueEntityList.add((MatchValue) result.get("e"));
            }
        }
    }

    public void clearAllData() {
        this.matchValueEntityList.clear();
        this.parameterMatchEntityList.clear();
        this.occurrenceEntityList.clear();
    }
}

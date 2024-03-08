package utils;

import burp.HttpResponse;
import com.github.wnameless.json.flattener.JsonFlattener;
import db.MatchHelperClass;
import db.entities.InputParameter;
import db.entities.InputValue;
import org.json.JSONException;
import org.jsoup.parser.Parser;

import java.util.*;

public class JsonParser extends ParserBase implements IParser {

    private HttpResponse response;
    private Hashtable<String, Collection<String>> keyValue;

    public JsonParser(){
        this.keyValue = new Hashtable<>();
    }

    @Override
    public boolean initialize(HttpResponse response) {
        this.response = response;

        String body = this.response.Body;
        // Remove Character to avoid exception
        // Unexpected character ('' (code 65279 / 0xfeff)): expected a valid value (number, String, array, object, 'true', 'false' or 'null')
        if(body.startsWith("\uFEFF"))
            body = body.substring(1);

        try{
            Map<String, Object> flattendJSON = JsonFlattener.flattenAsMap(body);
            this.fillKeyValue(flattendJSON);
        }
        catch(JSONException ex){
            Logger.getInstance().logToError(String.format("[%s] ERROR: The supplied JSON is not valid", this.getClass().getName()));
            return false;
        }

        return true;
    }

    private void fillKeyValue(Map<String, Object> flattendJSON){
        // Get all keys
        for(var key : flattendJSON.keySet()){
            var value = flattendJSON.get(key);
            if(value == null || value.toString().isEmpty()){
                continue;
            }

            if(!keyValue.containsKey(value.toString())){
                //New value not seen before
                Vector<String> jsonOccurrences = new Vector<String>();
                jsonOccurrences.add(key);
                keyValue.put(value.toString(), jsonOccurrences);
            }
            else{
                //Value already seen before, so add location to occurences list (multi-occurences)
                Vector<String> jsonOccurrences = (Vector<String>)keyValue.get(value.toString());
                jsonOccurrences.add(key);
                keyValue.put(value.toString(), jsonOccurrences);
            }
        }
    }

    public List<MatchHelperClass> matchAllOccurrences(InputParameter inputParameter, String messageHash){
        return this.performMatching(inputParameter, inputParameter.getOccurrenceEntities(), messageHash);
    }

    private List<MatchHelperClass> performMatching(InputParameter inputParameter, List<InputValue> occurrences, String messageHash) {
        var matches = new Vector<MatchHelperClass>();

        StringBuilder headers = new StringBuilder();
        for (var header : this.response.Headers) {
            headers.append(header.toString()).append("\n");
        }
        var decodedHeaders = Parser.unescapeEntities(headers.toString(), true);

        for(var occurrence : occurrences) {
            if (occurrence.isExcludedByNoiseReduction())
                continue;

            matches.addAll(matchHeaderFindings(this.response, decodedHeaders, occurrence, inputParameter, messageHash));

            if (!keyValue.containsKey(occurrence.getValue())){
                continue;
            }

            Collection<String> jsonOccurrences = keyValue.get(occurrence.getValue());
            for(var jsonLocationString: jsonOccurrences){
                String proof = String.format("JSON Match -> Path: %s contains value: %s", jsonLocationString, occurrence.getValue());
                matches.add(new MatchHelperClass(this.response, inputParameter.getName(), occurrence.getValue(), inputParameter.getType(),
                        this.response.ContentType, proof,
                        URLExtension.urlToString(this.response.AssociatedRequestUrl), messageHash, inputParameter, occurrence));
            }
        }

        return matches;
    }
}

package utils;

import burp.HttpResponse;
import db.MatchHelperClass;
import db.entities.InputParameter;
import db.entities.InputValue;
import org.jsoup.parser.Parser;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class DefaultParser extends ParserBase implements IParser {

    private HttpResponse currentResponse;
    private String currentResponseContent;

    @Override
    public boolean initialize(HttpResponse response) {
        try{
            this.currentResponse = response;
            this.currentResponseContent = this.currentResponse.Body;
            return true;
        }
        catch(Exception ex){
            Logger.getInstance().logToError(Arrays.toString(ex.getStackTrace()));
            return false;
        }
    }

    public List<MatchHelperClass> matchAllOccurrences(InputParameter inputParameter, String messageHash){
        return this.performMatching(inputParameter, inputParameter.getOccurrenceEntities(), messageHash);
    }

    private List<MatchHelperClass> performMatching(InputParameter inputParameter, List<InputValue> occurrences, String messageHash) {
        var matches = new Vector<MatchHelperClass>();
        var type = inputParameter.getType();

        StringBuilder headers = new StringBuilder();
        for (var header : this.currentResponse.Headers) {
            headers.append(header.toString()).append("\n");
        }
        var decodedHeaders = Parser.unescapeEntities(headers.toString(), true);

        for(var occurrence : occurrences.stream().filter(e -> !StringUtils.isNullOrEmpty(e.getValue())).collect(Collectors.toList())) {
            if (occurrence.isExcludedByNoiseReduction())
                continue;
            var value = occurrence.getValue();

            matches.addAll(matchHeaderFindings(this.currentResponse, decodedHeaders, occurrence, inputParameter, messageHash));

            var findings = findAllOccurrences(this.currentResponseContent, value);
            if (findings.size() != 0) {
                for (int idx : findings) {
                    var proof = surroundingText(this.currentResponseContent, value, idx);
                    var matcher = inputParameter.getRegexMatchingValueByIdentifier(occurrence.getIdentifier()).matcher(proof);
                    if (matcher.find()) {
                        matches.add(new MatchHelperClass(this.currentResponse, inputParameter.getName(), occurrence.getValue(), type,
                                this.currentResponse.ContentType, proof,
                                URLExtension.urlToString(this.currentResponse.AssociatedRequestUrl), messageHash, inputParameter, occurrence));
                    }
                }
            }
        }

        return matches;
    }
}

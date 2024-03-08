package utils;

import burp.HttpResponse;
import burp.api.montoya.MontoyaApi;
import db.MatchHelperClass;
import db.entities.InputParameter;
import db.entities.InputValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.util.Base64;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HtmlParser extends ParserBase implements IParser {

    private final Pattern BASE64_PATTERN = Pattern.compile("((?:[A-Za-z0-9+\\/]{4})*(?:[A-Za-z0-9+\\/]{2}==|[A-Za-z0-9+\\/]{3}=)?)");
    private MontoyaApi api;
    private Document currentDocument;
    private HttpResponse response;
    private List<String> base64Strings;

    public HtmlParser(MontoyaApi api){
        this.api = api;
    }

    public boolean initialize(HttpResponse response){
        this.response = response;
        var html = response.Body;
        if(html != null){
            try{
                currentDocument = Jsoup.parse(html);
                base64Strings = extractBase64Strings(currentDocument.html());
            }
            catch(Exception ex){
                Logger.getInstance().logToError(String.format("[%s] ERROR: %s", this.getClass().getName(), ex.getMessage()));
                return false;
            }
        }
        else{
            currentDocument = null;
        }
        return true;
    }

    private List<String> extractBase64Strings(String html){
        var results = new Vector<String>();
        var matcher = BASE64_PATTERN.matcher(html);
        if(matcher.find()){
            for(var match : matcher.results().collect(Collectors.toList())){
                var base64 = match.group(0);
                if(base64.isEmpty())
                    continue;
                String decoded;
                try{
                    decoded = new String(Base64.getDecoder().decode(base64));
                }
                catch(Exception ex){
                    continue;
                }
                results.add(decoded);
            }
        }
        return results;
    }

    public List<MatchHelperClass> matchAllOccurrences(InputParameter inputParameter, String messageHash){
            return this.performMatching(inputParameter, inputParameter.getOccurrenceEntities(), messageHash);
    }

    // performMatching2 uses new neo4j db model
    private List<MatchHelperClass> performMatching(InputParameter inputParameter, List<InputValue> occurrences, String messageHash) {
        var matches = new Vector<MatchHelperClass>();
        var type = inputParameter.getType();

        /** Directly match in the html **/
        var text = currentDocument.html();
        var encoded = this.api.utilities().urlUtils().encode(text);
        var decoded = this.api.utilities().urlUtils().decode(encoded);
        decoded = Parser.unescapeEntities(decoded, true);

        StringBuilder headers = new StringBuilder();
        for (var header : this.response.Headers) {
            headers.append(header.toString()).append("\n");
        }
        var decodedHeaders = Parser.unescapeEntities(headers.toString(), true);

        for(var occurrence : occurrences.stream().filter(e -> !StringUtils.isNullOrEmpty(e.getValue())).collect(Collectors.toList())) {
            if (occurrence.isExcludedByNoiseReduction())
                continue;
            var value = occurrence.getValue();

            var findings = findAllOccurrences(decoded, value);

            matches.addAll(matchHeaderFindings(this.response, decodedHeaders, occurrence, inputParameter, messageHash));

            if (findings.size() != 0) {
                for (int idx : findings) {
                    var proof = surroundingText(decoded, value, idx);
                    var matcher = inputParameter.getRegexMatchingValueByIdentifier(occurrence.getIdentifier()).matcher(proof);
                    if (matcher.find()) {
                        matches.add(new MatchHelperClass(this.response, inputParameter.getName(), occurrence.getValue(), type,
                                this.response.ContentType, proof,
                                URLExtension.urlToString(this.response.AssociatedRequestUrl), messageHash, inputParameter, occurrence));
                    }
                }
            }

            /** ParameterMatch in the Base64 Strings **/
            if (base64Strings != null && !base64Strings.isEmpty()) {
                for (var b64 : base64Strings) {
                    if (StringUtils.containsIgnoreCase(b64, value)) {
                        var proof = String.format("ParameterMatch in Base64: %s", base64Proof(b64));
//                        matches.add(new ParameterMatch(this.response, inputParameter, occurrence, proof, -1));
                        matches.add(new MatchHelperClass(this.response, inputParameter.getName(), occurrence.getValue(), type,
                                this.response.ContentType, proof,
                                URLExtension.urlToString(this.response.AssociatedRequestUrl), messageHash, inputParameter, occurrence));
                    }
                }
            }
        }

        return matches;
    }

    private String base64Proof(String b64){
        try{
            var encoded = Base64.getEncoder().encode(b64.getBytes());
            var stringValue = new String(encoded);
            return String.format("%s ...", stringValue.substring(0, 20));
        }
        catch(Exception ex){
            return "";
        }
    }
}

package burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import db.MatchHelperClass;
import db.entities.InputParameter;
import utils.*;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class HttpResponseParser {

    private MontoyaApi api;
    private IParser htmlParser;
    private IParser jsonParser;
    private IParser defaultParser;

    public HttpResponseParser(MontoyaApi api){
        this.api = api;
        this.htmlParser = new HtmlParser(this.api);
        this.jsonParser = new JsonParser();
        this.defaultParser = new DefaultParser();
    }

    public HttpResponse parseResponse(burp.api.montoya.http.message.responses.HttpResponse responseReceived, HttpRequest request){
        var raw = responseReceived.toByteArray().getBytes();

        var resp = this.parseResponseBase(responseReceived, raw, URLExtension.stringToUrl(request.url()));

        // Parse from burp.api request to own request type
        HttpRequestParser requestParser = new HttpRequestParser(this.api);
        burp.HttpRequest requestParsed = requestParser.parse(request);

        return resp;
    }
    // Takes response, request as bytes, request url
    public HttpResponse parseResponseBase(burp.api.montoya.http.message.responses.HttpResponse responseReceived, byte[] raw, URL requestUrl){
        var headers = responseReceived.headers();
        int statusCode = responseReceived.statusCode();

        //Extracts the Burp inferred Mime-Type as shown in the "MIME Type" column
        var contentType = responseReceived.inferredMimeType().toString();
        var body = extractBody(responseReceived, raw);

        return new HttpResponse(statusCode, body, contentType, headers, requestUrl);
    }
    private String extractBody(burp.api.montoya.http.message.responses.HttpResponse responseReceived, byte[] raw){
        var bodyOffset = responseReceived.bodyOffset();
        if(bodyOffset > 0 && bodyOffset < raw.length){
            var rawBody = Arrays.copyOfRange(raw, bodyOffset, raw.length);
            return new String(rawBody);
        }
        else {
            return "";
        }
    }

    public List<MatchHelperClass> getMatches(HttpResponse resp, List<InputParameter> inputParameters, String messageHash){
        return getMatchesSynchronized(resp, inputParameters, messageHash);
    }

    private List<MatchHelperClass> getMatchesSynchronized(HttpResponse resp, List<InputParameter> inputParameters, String messageHash){
        var matches = new Vector<MatchHelperClass>();
        IParser usedParser;

        if(resp.ContentType.equals("HTML")){
            usedParser = this.htmlParser;
        }
        else if(resp.ContentType.equals("JSON")){
            usedParser = this.jsonParser;
        }
        else{
            usedParser = this.defaultParser;
        }

        usedParser.initialize(resp);
        for(var param : inputParameters) {
            if (param.isExcludedByNoiseReduction()) {
                continue;
            }
            var newMatches = usedParser.matchAllOccurrences(param, messageHash);
            if(newMatches.size() > 0)
                matches.addAll(newMatches);
        }

        return matches;
    }
}

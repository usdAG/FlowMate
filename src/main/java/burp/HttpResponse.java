package burp;

import burp.api.montoya.http.message.HttpHeader;
import utils.Hashing;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

public class HttpResponse {

    public String Body;
    public String ContentType;
    public List<HttpHeader> Headers;
    public URL AssociatedRequestUrl;
    public int StatusCode;
    private String identifier;


    public HttpResponse(int statusCode, String body, String contentType, List<HttpHeader> headers){
        this.StatusCode = statusCode;
        this.Body = body;
        this.ContentType = contentType;
        this.Headers = headers;
        this.identifier = null;
    }

    public HttpResponse(int statusCode, String body, String contentType, List<HttpHeader> headers, URL requestUrl){
        this(statusCode, body, contentType, headers);
        this.AssociatedRequestUrl = requestUrl;
    }

    public String getResponseIdentifier(){
        if(identifier == null){
            calculateIdentifier();
        }
        return this.identifier;
    }

    private void calculateIdentifier(){
        List<String> headerAsString = Headers.stream().map(HttpHeader::toString).collect(Collectors.toList());
        String content = String.join("~", headerAsString);
        content += String.format("~BODY~%s", this.Body);
        this.identifier = Hashing.getSha512(content);
    }


}

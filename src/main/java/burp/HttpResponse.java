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

    public HttpResponse(int statusCode, String body, String contentType, List<HttpHeader> headers) {
        this.StatusCode = statusCode;
        this.Body = body;
        this.ContentType = contentType;
        this.Headers = headers;
    }

    public HttpResponse(int statusCode, String body, String contentType, List<HttpHeader> headers, URL requestUrl) {
        this(statusCode, body, contentType, headers);
        this.AssociatedRequestUrl = requestUrl;
    }
}

package burp;

import db.ParameterHelperClass;

import java.net.URL;
import java.util.Collection;

public class HttpRequest {

    public String Method;
    public URL Url;
    public Collection<ParameterHelperClass> parameterHelpers;

    public HttpRequest(String method, URL url, Collection<ParameterHelperClass> oldParameters){
        this.Method = method;
        this.Url = url;
        this.parameterHelpers = oldParameters;
    }

    @Override
    public String toString(){
        return String.format("%s\n%s\nParameters: %d", this.Method, this.Url.getPath(), this.parameterHelpers.size());
    }
}

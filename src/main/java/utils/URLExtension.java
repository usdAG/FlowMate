package utils;

import java.net.URL;

public class URLExtension {
    /**
     * Converts a java.net.URL to its string representation (only containing the host and path; not the query string)
     * @param url
     * @return
     */
    public static String urlToString(URL url){
        var path = url.getPath();
        var baseUrl = URLExtension.getBaseUrl(url);

        return String.format("%s%s", baseUrl, path);
    }

    /**
     * Creates the base url string consisting of protocol, host and port (if non-standard)
     * @param url
     * @return
     */
    public static String getBaseUrl(URL url){
        var proto = url.getProtocol();
        var host = url.getHost();
        int port = url.getPort();

        var printPort = true;
        if(proto.equals("http") && port == 80){
            printPort = false;
        }
        else if(proto.equals("https") && port == 443){
            printPort = false;
        } else if(port == -1) {
            printPort = false;
        }

        if(printPort){
            return String.format("%s://%s:%s", proto, host, port);
        }
        else{
            return String.format("%s://%s", proto, host);
        }
    }

}

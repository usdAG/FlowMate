package utils;

import burp.HttpResponse;
import db.MatchHelperClass;
import db.entities.InputParameter;
import db.entities.InputValue;

import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class ParserBase {

    private final int proofSurrounding = 30;

    protected Collection<Integer> findAllOccurrences(String text, String substring){
        Collection<Integer> indexes = new Vector<Integer>();
        int index = 0, substringLength = 0;

        //TODO Matching case sensitive?
        do{
            index = text.indexOf(substring, index + substringLength);
            if(index != -1)
                indexes.add(index);

            substringLength = substring.length();
        }
        while(index != -1);

        return indexes;
    }

    protected String surroundingText(String text, String substring, int index){
        var beginning = Math.max((index - proofSurrounding), 0);
        var ending = Math.min((index + proofSurrounding + substring.length()), text.length());
        return text.substring(beginning, ending);
    }

    protected List<MatchHelperClass> matchHeaderFindings(HttpResponse response, String decodedHeaders,
                                                       InputValue inputValue, InputParameter inputParameter, String messageHash) {
        var matches = new Vector<MatchHelperClass>();

        var matcher = inputParameter.getRegexForHeaderMatchingValueByIdentifier(inputValue.getIdentifier()).matcher(decodedHeaders);
        while (matcher.find()) {
            String matchedHeader = matcher.group(1);
            if (isCustomHeader(matchedHeader)) {
                var proof = "Header: " + matchedHeader + ": " + matcher.group(2);
                matches.add(new MatchHelperClass(response, inputParameter.getName(), inputValue.getValue(), inputParameter.getType(),
                        response.ContentType, proof,
                        URLExtension.urlToString(response.AssociatedRequestUrl), messageHash, inputParameter, inputValue));
            }
        }

        return matches;
    }

    protected boolean isCustomHeader(String headerName) {
        // List of common headers to exclude
        String[] commonHeaders = {
                "Date",
                "Server",
                "X-Powered-By",
                "Expires",
                "Cache-Control",
                "Pragma",
                "X-Frame-Options",
                "Vary",
                "Content-Length",
                "Connection",
                "Content-Type",
                "Content-Language",
                "Accept-Ranges",
                "Last-Modified",
                "Etag"
                // Add more common headers as needed
        };

        for (String commonHeader : commonHeaders) {
            if (headerName.equalsIgnoreCase(commonHeader)) {
                return false;
            }
        }

        return true;
    }
}

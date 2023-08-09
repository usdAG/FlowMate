package utils;

import java.util.Collection;
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
}

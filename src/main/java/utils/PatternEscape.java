package utils;

import java.util.regex.Pattern;

public class PatternEscape {

    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    public static String escapeForRegex(String input){
        var matcher = PatternEscape.SPECIAL_REGEX_CHARS.matcher(input);
        return matcher.replaceAll("\\\\$0");
    }
}

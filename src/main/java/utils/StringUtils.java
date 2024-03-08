package utils;

import java.util.List;

public class StringUtils {

    public static boolean isNullOrEmpty(String input){
        return input == null || input.isEmpty();
    }

    public static boolean containsIgnoreCase(String a, String b){
        return a.toLowerCase().contains(b.toLowerCase());
    }

    public static boolean containsListIgnoreCase(List<String> base, String pattern){
        for(var baseString : base){
            if (pattern.contains(baseString)) {
                return true;
            }
        }
        return false;
    }
}

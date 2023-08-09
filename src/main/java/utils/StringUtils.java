package utils;

public class StringUtils {

    public static boolean isNullOrEmpty(String input){
        return input == null || input.isEmpty();
    }

    public static boolean containsIgnoreCase(String a, String b){
        return a.toLowerCase().contains(b.toLowerCase());
    }

}

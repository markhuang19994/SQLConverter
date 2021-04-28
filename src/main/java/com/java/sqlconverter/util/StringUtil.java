package com.java.sqlconverter.util;

import java.util.List;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public final class StringUtil {

    private StringUtil() {
    }

    public static String[] wordsReplace(String origin, String result, String... words) {
        String[] newWords = new String[words.length];
        for (int i = 0; i < words.length; i++) {
            newWords[i] = words[i].replace(origin, result);
        }
        return newWords;
    }

    public static String abbreviateString(String str, int maxLen) {
        return str.length() >= maxLen ? str.substring(0, maxLen) + "..." : str;
    }

    public static String[] abbreviateString(String[] strs, int maxLen) {
        String[] newStrs = new String[strs.length];
        for (int i = 0; i < strs.length; i++) {
            newStrs[i] = strs[i].length() >= maxLen ? strs[i].substring(0, maxLen) + "..." : strs[i];
        }
        return newStrs;
    }

    public static boolean checkStringMatchRegexList(String str, List<String> regexList) {
        for (String regex : regexList) {
            if (str.matches(regex)) {
                return true;
            }
        }
        return false;
    }
    
    public static int indexOfIgnoreCase(final String haystack,
                                        final String needle) {
        if (needle.isEmpty() || haystack.isEmpty()) {
            // Fallback to legacy behavior.
            return haystack.indexOf(needle);
        }
        
        for (int i = 0; i < haystack.length(); ++i) {
            // Early out, if possible.
            if (i + needle.length() > haystack.length()) {
                return -1;
            }
            
            // Attempt to match substring starting at position i of haystack.
            int j = 0;
            int ii = i;
            while (ii < haystack.length() && j < needle.length()) {
                char c = Character.toLowerCase(haystack.charAt(ii));
                char c2 = Character.toLowerCase(needle.charAt(j));
                if (c != c2) {
                    break;
                }
                j++;
                ii++;
            }
            // Walked all the way to the end of the needle, return the start
            // position that this was found.
            if (j == needle.length()) {
                return i;
            }
        }
        
        return -1;
    }
}

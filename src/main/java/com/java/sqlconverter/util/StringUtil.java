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

    public static boolean checkStringMatchRegexs(String str, List<String> regexs) {
        for (String regex : regexs) {
            if (str.matches(regex)) {
                return true;
            }
        }
        return false;
    }
}

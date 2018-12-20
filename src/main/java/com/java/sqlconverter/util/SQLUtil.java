package com.java.sqlconverter.util;

import com.java.sqlconverter.converter.impl.InsertConverterImpl;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/19, MarkHuang,new
 * </ul>
 * @since 2018/12/19
 */
public class SQLUtil {

//    public static String complementDummyInsertSemicolon(String str, String replacement) {
//        char[] chars = str.toCharArray();
//        Pattern p = Pattern.compile("((?i)insert)\\s+(((?i)into)\\s+)?(.*?)\\s+\\(([\\s\\S]*?)\\)\\s+((?i)values)");
//        Matcher m = p.matcher(str);
//        List<Integer> appendSemicolonIndex = new ArrayList<>();
//        while (m.find()) {
//            int rightBracketsCount = 0;
//            int leftBracketsCount = 0;
//            boolean isInQuotation = false;
//            boolean isEscape = false;
//            int end = m.end();
//            if (++end >= chars.length) {
//                break;
//            }
//            int x = end;
//            while (x < chars.length) {
//                x++;
//                char c = chars[end];
//                if (isEscape) {
//                    isEscape = false;
//                    continue;
//                }
//                if (c == '\\') {
//                    isEscape = true;
//                } else if (c == '\'') {
//                    isInQuotation = !isInQuotation;
//                } else if (c == '(') {
//                    if (isInQuotation) {
//                        continue;
//                    }
//                    leftBracketsCount++;
//                } else if (c == ')') {
//                    if (isInQuotation) {
//                        continue;
//                    }
//                    if (leftBracketsCount == ++rightBracketsCount) {
//                        if (end + 1 <= chars.length && chars[end + 1] != ';') {
//                            appendSemicolonIndex.add(end);
//                        }
//                        break;
//                    }
//                }
//                end++;
//            }
//        }
//        StringBuilder sb = new StringBuilder();
//        int nowIndex = 0;
//        for (Integer semicolonIndex : appendSemicolonIndex) {
//            sb.append(str.substring(nowIndex, semicolonIndex + 1)).append(replacement);
//            nowIndex = semicolonIndex + 1;
//        }
//        sb.append(str.substring(nowIndex));
//        return sb.toString();
//    }


    private static final Map<String, String> sensitiveWordsConvertMap;

    static {
        sensitiveWordsConvertMap = new HashMap<>();
        sensitiveWordsConvertMap.put(",", "#$#01#$#");
        sensitiveWordsConvertMap.put(");", "#$#02#$#");
    }

    public static String complementDummyInsertSemicolonAndReplaceSensitiveWordsInInsertValues(String str) {
        char[] chars = str.toCharArray();
        Pattern p = Pattern.compile("((?i)insert)\\s+(((?i)into)\\s+)?(.*?)\\s+\\(([\\s\\S]*?)\\)\\s+((?i)values)");
        Matcher m = p.matcher(str);
        Map<Integer, String> appendWordsIndex = new LinkedHashMap<>();
        while (m.find()) {
            int rightBracketsCount = 0;
            int leftBracketsCount = 0;
            boolean isInQuotation = false;
            boolean isEscape = false;
            int end = m.end();
            if (++end >= chars.length) {
                break;
            }
            for (; end < chars.length; end++) {
                char c = chars[end];
                if (isEscape) {
                    isEscape = false;
                    continue;
                }
                if (c == '\\') {
                    isEscape = true;
                } else if (c == '\'') {
                    if (isInQuotation && end + 1 < chars.length && chars[end + 1] == '\'') {//如果在字串裡面的"''"就當跳脫字元
                        isEscape = true;
                    } else {
                        isInQuotation = !isInQuotation;
                    }
                } else if (c == ',') {
                    if (isInQuotation) {
                        appendWordsIndex.put(end, sensitiveWordsConvertMap.get(","));
                    }
                } else if (c == '(') {
                    if (isInQuotation) {
                        continue;
                    }
                    leftBracketsCount++;
                } else if (c == ')') {
                    if (isInQuotation) {
                        if (end + 1 < chars.length && chars[end + 1] == ';') {
                            appendWordsIndex.put(end, sensitiveWordsConvertMap.get(");"));
                            appendWordsIndex.put(end + 1, "");
                            isEscape = true;
                        }
                        continue;
                    }
                    if (leftBracketsCount == ++rightBracketsCount) {
                        if (end + 1 <= chars.length && chars[end + 1] != ';') {
                            appendWordsIndex.put(end, InsertConverterImpl.DUMMY_SEMICOLON);
                        }
                        break;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        int nowIndex = 0;
        for (Integer wordIndex : appendWordsIndex.keySet()) {
            sb.append(str.substring(nowIndex, wordIndex)).append(appendWordsIndex.get(wordIndex));
            nowIndex = wordIndex + 1;
        }
        sb.append(str.substring(nowIndex));
        return sb.toString();
    }
}

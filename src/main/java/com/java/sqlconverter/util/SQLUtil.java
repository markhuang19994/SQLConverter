package com.java.sqlconverter.util;

import com.java.sqlconverter.converter.impl.InsertConverterImpl;
import com.java.sqlconverter.model.CommentCheckReport;

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
public final class SQLUtil {

    private SQLUtil() {
    }

    private static final Map<String, String> INSERT_SENSITIVE_WORDS_CONVERT_MAP;

    static {
        INSERT_SENSITIVE_WORDS_CONVERT_MAP = new HashMap<>();
        INSERT_SENSITIVE_WORDS_CONVERT_MAP.put(",", "#$#01#$#");
        INSERT_SENSITIVE_WORDS_CONVERT_MAP.put(");", "#$#02#$#");
    }

    /**
     * 1.將insert ... values(*)語句中*的內容的敏感文字替換','與');'
     *
     * 2.自動補齊每一句insert的分號
     *
     * @param sqlText sqlText
     * @return String
     */
    public static String complementDummyInsertSemicolonAndReplaceSensitiveWordsInInsertValues(String sqlText) {
        char[] chars = sqlText.toCharArray();
        Pattern p = Pattern.compile("((?i)insert)\\s+(((?i)into)\\s+)?(.*?)\\s+\\(([\\s\\S]*?)\\)\\s+((?i)values)");
        Matcher m = p.matcher(sqlText);
        Map<Integer, String> appendWordsIndex = new LinkedHashMap<>();
        while (m.find()) {
            int rightBracketsCount = 0;
            int leftBracketsCount = 0;
            boolean isInQuotation = false;
            boolean isEscape = false;
            int end = m.end();
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
                        appendWordsIndex.put(end, INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(","));
                    }
                } else if (c == '(') {
                    if (isInQuotation) {
                        continue;
                    }
                    leftBracketsCount++;
                } else if (c == ')') {
                    if (isInQuotation) {
                        if (end + 1 < chars.length && chars[end + 1] == ';') {
                            appendWordsIndex.put(end, INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(");"));
                            appendWordsIndex.put(end + 1, "");
                            isEscape = true;
                        }
                        continue;
                    }
                    if (leftBracketsCount == ++rightBracketsCount) {
                        if (end + 1 <= chars.length && chars[end + 1] != ';') {
                            appendWordsIndex.put(end + 1, InsertConverterImpl.DUMMY_SEMICOLON + "\n");
                        }
                        break;
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        int nowIndex = 0;
        for (Integer wordIndex : appendWordsIndex.keySet()) {
            sb.append(sqlText.substring(nowIndex, wordIndex)).append(appendWordsIndex.get(wordIndex));
            nowIndex = wordIndex + 1;
        }
        sb.append(sqlText.substring(nowIndex));
        return sb.toString();
    }

    /**
     * 把之前被替換掉的敏感詞彙換回
     * @param sqlText sqlText
     * @return String
     */
    public static String recoverInsertSql(String sqlText) {
        for (String sensitiveWord : INSERT_SENSITIVE_WORDS_CONVERT_MAP.keySet()) {
            sqlText = sqlText.replace(INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(sensitiveWord), sensitiveWord);
        }
        return sqlText
                .replace(InsertConverterImpl.DUMMY_SEMICOLON, "");
    }

    public static String generateErrorMessageFromReports(List<CommentCheckReport> reports, boolean noMatterPass) {
        StringBuilder sb = new StringBuilder("錯誤訊息:").append(System.lineSeparator());
        for (CommentCheckReport commentCheckReport : reports) {
            if (noMatterPass || !commentCheckReport.isPass()) {
                List<String> errorMessages = commentCheckReport.getErrorMessages();
                for (String errorMessage : errorMessages) {
                    sb.append(errorMessage).append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }

    public static String removeUpsertComments(String sqlText){
        return sqlText.replaceAll("--@\\s*upsert\\s*:.*?\\s*?\n", "\n");
    }
}

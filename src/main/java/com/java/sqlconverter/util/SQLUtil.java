package com.java.sqlconverter.util;

import com.java.sqlconverter.converter.impl.InsertConverterImpl;
import com.java.sqlconverter.model.CommentCheckReport;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        INSERT_SENSITIVE_WORDS_CONVERT_MAP.put(")", "#$#02#$#");
    }
    
    public static String replaceStatementSensitiveWord(String insertSql) {
        final Map<Integer, String> appendWordsIndex = new LinkedHashMap<>();
        
        boolean isInQuotation = false;
        boolean isEscape = false;
        
        final char[] chars = insertSql.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            if (isEscape) {
                isEscape = false;
                continue;
            }
            
            if (c == '\\') {
                if (isInQuotation) {
                    isEscape = true;
                }
            } else if (c == '\'') {
                if (isInQuotation && i + 1 < chars.length && chars[i + 1] == '\'') {
                    isEscape = true;
                } else {
                    isInQuotation = !isInQuotation;
                }
            } else {
                if (isInQuotation) {
                    final String sensitive = INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(String.valueOf(c));
                    if (sensitive != null) {
                        appendWordsIndex.put(i, sensitive);
                    }
                }
            }
        }
    
        StringBuilder sb = new StringBuilder();
        int nowIndex = 0;
        for (Integer wordIndex : appendWordsIndex.keySet()) {
            sb.append(insertSql, nowIndex, wordIndex).append(appendWordsIndex.get(wordIndex));
            nowIndex = wordIndex + 1;
        }
        sb.append(insertSql.substring(nowIndex));
        return sb.toString();
    }
    
    /**
     * 把之前被替換掉的敏感詞彙換回
     *
     * @param statement sqlText
     * @return String
     */
    public static String recoverStatementSensitiveWord(String statement) {
        for (String sensitiveWord : INSERT_SENSITIVE_WORDS_CONVERT_MAP.keySet()) {
            statement = statement.replace(INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(sensitiveWord), sensitiveWord);
        }
        return statement;
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
    
    public static String removeUpsertComments(String sqlText) {
        return sqlText.replaceAll("--@\\s*upsert\\s*:.*?\\s*?\n", "\n");
    }
}

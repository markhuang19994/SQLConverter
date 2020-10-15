package com.java.sqlconverter.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * @param sqlText sqlText
     * @return String
     */
    public static String recoverStatementSensitiveWord(String sqlText) {
        for (String sensitiveWord : INSERT_SENSITIVE_WORDS_CONVERT_MAP.keySet()) {
            sqlText = sqlText.replace(INSERT_SENSITIVE_WORDS_CONVERT_MAP.get(sensitiveWord), sensitiveWord);
        }
        return sqlText;
    }
    
    public static String removeUpsertComments(String sqlText) {
        return sqlText.replaceAll("--@\\s*upsert\\s*:.*?\\s*?\n", "\r\n");
    }
}

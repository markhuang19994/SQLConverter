package com.java.sqlconverter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    
    public static String removeUpsertComments(String sqlText) {
        return sqlText.replaceAll("--@\\s*upsert\\s*:.*?\\s*?\n", "\r\n");
    }
    
    public static Result parseParamInSentence(String sentence) {
        final List<String> paramList = new ArrayList<>();
        
        final StringBuilder tempSb = new StringBuilder();
        final char[] charArray = sentence.toCharArray();
        
        boolean isInQuotation = false;
        int pareParentheses = 0;
        boolean isEscape = false;
        
        int idx = 0;
        for (; idx < charArray.length; idx++) {
            final char c = charArray[idx];
            tempSb.append(c);
    
            if (isEscape) {
                isEscape = false;
                continue;
            }
            
            switch (c) {
                //如果遇到單引號，代表字串開始
                case '\'':
                    //如果已經在單引號裡面又遇到單引號
                    if (isInQuotation) {
                        //如果發現下一個字元又是單引號，代表這個單引號一定是跳脫字元
                        if (idx + 1 < charArray.length && charArray[idx + 1] == '\'') {
                            isEscape = true;
                        } else { //如果下一個字元不是單引號，代表這個單引號一定是字串結尾
                            isInQuotation = false;
                        }
                    } else {
                        isInQuotation = true;
                    }
                    break;
                case '\\':
                    isEscape = true;
                    break;
                case '(':
                    //如果在字串外面遇到左括號，就預期在一對括號的開始
                    if (!isInQuotation) {
                        pareParentheses++;
                    }
                    break;
                case ')':
                    //如果在字串外面遇到右括號，就預期是一對括號的結束
                    if (!isInQuotation) {
                        pareParentheses--;
                        if (pareParentheses == -1) {
                            break;
                        }
                    }
                    break;
                case ',':
                    //如果遇到逗號，且當前狀態不在單引號或括號裡面，那在遇到逗號前儲存的字串(tempSb)一定是參數
                    if (pareParentheses == 0 && !isInQuotation) {
                        paramList.add(tempSb.substring(0, tempSb.length() -1));
                        tempSb.setLength(0);
                    }
                    break;
            }
        }
    
        paramList.add(tempSb.toString());
    
        return new Result(paramList, idx);
    }
    
    public static class Result {
        public final List<String> paramList;
        public final int          lastIndex;
        
        private Result(List<String> paramList, int lastIndex) {
            this.paramList = paramList;
            this.lastIndex = lastIndex;
        }
    }
}

package com.java.sqlconverter.util;

import java.util.ArrayList;
import java.util.List;

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
        if (sentence.indexOf("(") != 0) {
            throw new RuntimeException("sentence first index must be '(' but is :" + sentence);
        }
    
        sentence = sentence.substring(1);
        
        final List<String> paramList = new ArrayList<>();
        
        final StringBuilder tempSb = new StringBuilder();
        final char[] charArray = sentence.toCharArray();
        
        boolean isInQuotation = false;
        int pareParentheses = 0;
        boolean isEscape = false;
    
        int idx = 0;
        for (; idx < charArray.length; idx++) {
            if (isEscape) {
                isEscape = false;
                continue;
            }
            
            final char c = charArray[idx];
            
            switch (c) {
                //如果遇到單引號，代表字串開始
                case '\'':
                    //如果已經在單引號裡面又遇到單引號
                    if (isInQuotation) {
                        //如果發現下一個字元又是單引號，代表這個單引號一定是跳脫字元
                        if (idx + 1 < charArray.length && charArray[idx + 1] == '\'') {
                            isEscape = true;
                        } else { //如果下一個字元不是單引號，代表這個單引號一定是字串結尾
                            if (pareParentheses != 0) {
                                paramList.add(tempSb.toString());
                                tempSb.setLength(0);
                            }
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
                    tempSb.append(c);
                    break;
                case ')':
                    //如果在字串外面遇到右括號，就預期是一對括號的結束
                    if (!isInQuotation) {
                        pareParentheses--;
                        //如果括號都結束了，代表這是一個函數之類的東西，像是:
                        //insert into ... value (func(cast(), ()), '', '')的func函數
                        //所以預期當前暫存的值是insert的一個項目，放入insertList
                        if (pareParentheses == 0) {
                            paramList.add(tempSb.toString());
                            tempSb.setLength(0);
                        } else if (pareParentheses == -1) {
                            break;
                        }
                    }
                    tempSb.append(c);
                    break;
                default:
                    tempSb.append(c);
            }
        }
        
        return new Result(paramList, idx);
    }
    
    public static class Result {
        public final List<String> paramList;
        public final int lastIndex;
    
        private Result(List<String> paramList, int lastIndex) {
            this.paramList = paramList;
            this.lastIndex = lastIndex;
        }
    }
}

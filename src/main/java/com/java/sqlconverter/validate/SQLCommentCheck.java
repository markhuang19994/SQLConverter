package com.java.sqlconverter.validate;

import com.java.sqlconverter.model.SQLCommentCheckerReport;
import com.java.sqlconverter.model.SQLDetails;

import java.util.*;

/**
 * 檢查註釋是否正確,且不考慮順序及邏輯問題(如:--{}update後才--{}init),
 * 如果檢查不通過則顯示錯誤行數以及正確關鍵字提示
 *
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class SQLCommentCheck {
    private static SQLCommentCheck sqlCommentCheck = new SQLCommentCheck();
    private static final String KEY_WORD_1 = "--{}";
    private static final List<String> KEY_WORD_1_REGEX = Arrays.asList(
            "\\s*?init\\s*?", "\\s*?update\\s*?",
            "\\s*?SYS_TYPE\\s*?", "\\s*?TRANS_TYPE\\s*?", "\\s*?NORMAL_TYPE\\s*?"
    );
    private static final String KEY_WORD_2 = "--@";
    private static final List<String> KEY_WORD_2_REGEX = Arrays.asList(
            "\\s*?pk\\s*?:\\s*?.+",
            "\\s*?upsert\\s*?:\\s*?(on|off)"
    );

    private static final String KEY_WORD1_HINT = "init或update或SYS_TYPE或TRANS_TYPE或NORMAL_TYPE";
    private static final String KEY_WORD2_HINT = "pk:xxx或upsert:on或upsert:off";

    private SQLCommentCheck() {
    }

    public static SQLCommentCheck getInstance() {
        return SQLCommentCheck.sqlCommentCheck;
    }

    public SQLCommentCheckerReport check(SQLDetails sqlDetails) {
        String sqlFileText = sqlDetails.getSqlFileText();
        Map<String, List<Integer>> errorMap = new LinkedHashMap<>();
        List<Integer> keyWord1ErrorLine = new ArrayList<>();
        List<Integer> keyWord2ErrorLine = new ArrayList<>();
        List<KeyWordsAndLine> keyWordsAndLineList = new ArrayList<>();
        boolean hasError = false;
        int nowLine = 1;
        for (String lineText : sqlFileText.split("\n")) {
            if (!checkKeyWord1(lineText)) {
                hasError = true;
                keyWord1ErrorLine.add(nowLine);
            }
            if (!checkKeyWord2(lineText)) {
                hasError = true;
                keyWord2ErrorLine.add(nowLine);
            }
            keyWordsAndLineList.add(new KeyWordsAndLine(lineText.replaceAll("\\s", ""), nowLine));
            nowLine++;
        }
        errorMap.put(KEY_WORD_1, keyWord1ErrorLine);
        errorMap.put(KEY_WORD_2, keyWord2ErrorLine);
        Map<String, List<Integer>> rationalError = checkRational(keyWordsAndLineList);
        if (rationalError.keySet().size() > 0) {
            hasError = true;
        }
        errorMap.putAll(rationalError);
        return new SQLCommentCheckerReport(hasError, errorMap);
    }

    /**
     * error map的value定義為第幾行
     * error map的key定義為錯誤原因或者特定的KEY_WORD(--{}/--@),他們的行為如下:
     * 1.keyword1(--{})或keyword2(--@)代表語法錯誤,顯示錯誤訊息統一為KEY_WORD1_HINT或KEY_WORD2_HINT
     * 2.一般的key,如果行數為-1則錯誤訊息中不顯示在第幾行
     *
     * @param errorMap errorMap<>
     * @return String
     */
    public String generateErrorMessage(Map<String, List<Integer>> errorMap) {
        StringBuilder sb = new StringBuilder("\n");
        List<Integer> keyWord1ErrorLine = errorMap.get(KEY_WORD_1);
        if (keyWord1ErrorLine != null && keyWord1ErrorLine.size() != 0) {
            sb.append(String.format("--{}後面可以接:%s, 錯誤在行:%s", KEY_WORD1_HINT, keyWord1ErrorLine)).append("\n");
        }
        List<Integer> keyWord2ErrorLine = errorMap.get(KEY_WORD_2);
        if (keyWord2ErrorLine != null && keyWord2ErrorLine.size() != 0) {
            sb.append(String.format("--@後面可以接:%s, 錯誤在行:%s", KEY_WORD2_HINT, keyWord2ErrorLine)).append("\n");
        }
        errorMap.remove(KEY_WORD_1);
        errorMap.remove(KEY_WORD_2);
        for (String reason : errorMap.keySet()) {
            Integer line = errorMap.get(reason).get(0);
            if (line == -1) {
                sb.append(String.format("註解錯誤, 原因:%s", reason)).append("\n");
            } else {
                sb.append(String.format("註解錯誤, 原因:%s,在行:%d", reason, line)).append("\n");
            }
        }
        return sb.toString();
    }

    private boolean checkKeyWord1(String line) {
        if (line.indexOf(KEY_WORD_1) == 0) {
            return checkLineWithRegex(line.substring(KEY_WORD_1.length()), KEY_WORD_1_REGEX);
        }
        return true;
    }

    private boolean checkKeyWord2(String line) {
        if (line.indexOf(KEY_WORD_2) == 0) {
            return checkLineWithRegex(line.substring(KEY_WORD_2.length()), KEY_WORD_2_REGEX);
        }
        return true;
    }

    private boolean checkLineWithRegex(String line, List<String> regex) {
        for (String keyWord1Regex : regex) {
            if (line.matches(keyWord1Regex)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<Integer>> checkRational(List<KeyWordsAndLine> keyWordsAndLineList) {
        Map<String, List<Integer>> errorMap = new LinkedHashMap<>();
        boolean isUpdateDeclare = false;
        boolean isInitDeclare = false;
        boolean isTypeDeclare = false;
        int upserOnCount = 0;
        int upserOffCount = 0;
        boolean isPkDeclare = false;
        for (KeyWordsAndLine keyWordsAndLine : keyWordsAndLineList) {
            String keyWord = keyWordsAndLine.keyWord;
            int line = keyWordsAndLine.line;
            if (keyWord.indexOf("--{}") == 0) {
                if (keyWord.contains("init")) {
                    if (isUpdateDeclare) {
                        errorMap.put("初始化應該放置在更新前", Collections.singletonList(line));
                    }
                    isInitDeclare = true;
                } else if (keyWord.contains("update")) {
                    isUpdateDeclare = true;
                } else if (keyWord.contains("SYS_TYPE")
                        || keyWord.contains("TRANS_TYPE")
                        || keyWord.contains("NORMAL_TYPE")) {
                    if (isInitDeclare || isUpdateDeclare) {
                        errorMap.put(
                                "類型區塊應該放置在檔案最前面",
                                Collections.singletonList(line)
                        );
                    }
                    isTypeDeclare = true;
                }
            } else if (keyWord.indexOf("--@") == 0) {
                if (keyWord.contains("pk")) {
                    isPkDeclare = true;
                } else if (keyWord.matches("--@\\s*upsert\\s*:\\s*on")) {
                    if (!isPkDeclare) {
                        errorMap.put(
                                "在宣告upsert:on前必須宣告pk(primary key)",
                                Collections.singletonList(line)
                        );
                    }
                    upserOnCount += 1;
                } else if (keyWord.matches("--@\\s*upsert\\s*:\\s*off")) {
                    if (!isPkDeclare) {
                        errorMap.put(
                                "在宣告upsert:off前必須宣告pk(primary key)",
                                Collections.singletonList(line)
                        );
                    }
                    upserOffCount++;
                    if (upserOnCount < upserOffCount) {
                        errorMap.put(
                                "uppsert on off 順序錯誤",
                                Collections.singletonList(line)
                        );
                    }
                }
            }
            if (!isTypeDeclare) {
                errorMap.put(
                        "類型區塊未宣告{SYS_TYPE/TRANS_TYPE/NORMAL_TYPE}",
                        Collections.singletonList(-1)
                );
            }
        }
        return errorMap;
    }

    class KeyWordsAndLine {
        private String keyWord;
        private int line;

        public KeyWordsAndLine(String keyWord, int line) {
            this.keyWord = keyWord;
            this.line = line;
        }
    }

}

package com.java.sqlconverter;

import com.java.sqlconverter.model.CommentCheckReport;
import com.java.sqlconverter.util.StringUtil;
import com.java.sqlconverter.validate.CommentAndLine;
import com.java.sqlconverter.validate.CommentRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/21, MarkHuang,new
 * </ul>
 * @since 2018/12/21
 */
public class CommentRuleByAndy implements CommentRule {
    private final List<String> regexs = Arrays.asList(
            "--\\{}\\s*?init\\s*?", "--\\{}\\s*?update\\s*?",
            "--\\{}\\s*?SYS_TYPE\\s*?", "--\\{}\\s*?TRANS_TYPE\\s*?",
            "--\\{}\\s*?NORMAL_TYPE\\s*?"
    );

    @Override
    public CommentCheckReport checkComment(List<CommentAndLine> commentAndLines) {
        List<String> errorMessages = new ArrayList<>();
        for (CommentAndLine commentAndLine : commentAndLines) {
            String comment = commentAndLine.getComment();
            int line = commentAndLine.getLine();
            if (comment.indexOf("--{}") == 0) {
                boolean isPass = StringUtil.checkStringMatchRegexs(comment, regexs);
                if (!isPass) {
                    String correctFormat = "--{} + init|update|SYS_TYPE|TRANS_TYPE|NORMAL_TYPE";
                    errorMessages.add(String.format("第%d行:%s格式錯誤,正確格式為:%s",
                            line, comment, correctFormat
                    ));
                }
            }
        }
        errorMessages.addAll(checkLogicRational(commentAndLines));
        return new CommentCheckReport(errorMessages.size() == 0, errorMessages);
    }

    @Override
    public boolean isLineNeedCheck(String line) {
        return line.matches("^(--\\{}.*)$");
    }

    private List<String> checkLogicRational(List<CommentAndLine> commentAndLines) {
        List<String> errorMessages = new ArrayList<>();
        boolean isUpdateDeclare = false;
        boolean isInitDeclare = false;
        boolean isTypeDeclare = false;
        for (CommentAndLine commentAndLine : commentAndLines) {
            String comment = commentAndLine.getComment();
            int line = commentAndLine.getLine();
            if (comment.contains("init")) {
                if (isUpdateDeclare) {
                    errorMessages.add(String.format("第%d行:初始化邏輯錯誤,%s", line, "初始化應該放置在更新前"));
                }
                isInitDeclare = true;
            } else if (comment.contains("update")) {
                isUpdateDeclare = true;
            } else if (comment.contains("SYS_TYPE")
                    || comment.contains("TRANS_TYPE")
                    || comment.contains("NORMAL_TYPE")) {
                if (isInitDeclare || isUpdateDeclare) {
                    errorMessages.add(String.format("第%d行:%s", line, "類型區塊應該放置在檔案最前面"));
                }
                isTypeDeclare = true;
            }
        }
        if (!isTypeDeclare) {
            errorMessages.add("類型區塊未宣告{SYS_TYPE/TRANS_TYPE/NORMAL_TYPE}");
        }
        return errorMessages;
    }
}

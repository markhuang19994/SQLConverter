package com.java.sqlconverter.validate;

import com.java.sqlconverter.model.CommentCheckReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只需要實作CommentRule並且註冊進此類,就能自動對sql文件驗證
 *
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class SQLCommentCheck {

    private List<CommentRule> commentRules = new ArrayList<>();
    private Map<String, List<CommentAndLine>> rulesCommentAndLines = new LinkedHashMap<>();
    private List<CommentCheckReport> commentCheckReports = new ArrayList<>();
    private String sqlText;
    private boolean isAllPass = true;

    public SQLCommentCheck(String sqlText) {
        this.sqlText = sqlText;
    }

    public void register(CommentRule rule) {
        commentRules.add(rule);
    }

    /**
     * 產出commentRule需要驗證的comment與其行號,
     * 如果isLineNeedCheck回傳true,代表此commentRule需要驗證這一行
     *
     * @return SQLCommentCheck
     */
    public SQLCommentCheck generateCommentAndLine() {
        String[] lineTexts = this.sqlText.split("\n");
        for (CommentRule rule : commentRules) {
            int i = 1;
            for (String lineText : lineTexts) {
                if (rule.isLineNeedCheck(lineText)) {
                    addAndCreateMapList(rulesCommentAndLines, rule.toString(), new CommentAndLine(lineText, i));
                }
                i++;
            }
        }
        return this;
    }

    /**
     * 呼叫已經註冊的commentRule的check方法,並收集回傳的report
     *
     * @return List<CommentCheckReport>
     */
    public List<CommentCheckReport> processCommentRule() {
        for (CommentRule commentRule : this.commentRules) {
            List<CommentAndLine> commentAndLines = rulesCommentAndLines.get(commentRule.toString());
            commentAndLines = commentAndLines == null ? new ArrayList<>() : commentAndLines;
            CommentCheckReport commentCheckReport = commentRule.checkComment(commentAndLines);
            if (!commentCheckReport.isPass()) {
                isAllPass = false;
            }
            this.commentCheckReports.add(commentCheckReport);
        }
        return this.commentCheckReports;
    }

    public List<CommentCheckReport> getCommentCheckReports() {
        return this.commentCheckReports;
    }

    public boolean isAllPass() {
        return isAllPass;
    }

    private <T> void addAndCreateMapList(Map<String, List<T>> m, String key, T target) {
        List<T> list = m.get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.add(target);
        m.put(key, list);
    }

}


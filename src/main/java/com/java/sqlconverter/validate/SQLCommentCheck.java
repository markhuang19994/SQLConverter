package com.java.sqlconverter.validate;

import com.java.sqlconverter.util.CommentCheckReport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
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

    public SQLCommentCheck generateCommentAndLine() {
        String[] lines = this.sqlText.split("\n");
        for (CommentRule rule : commentRules) {
            List<String> commentRegex = rule.getCommentRegex();
            int i = 1;
            for (String line : lines) {
                for (String regex : commentRegex) {
                    if (line.matches(regex)) {
                        addAndCreateMapList(rulesCommentAndLines, rule.toString(), new CommentAndLine(line, i));
                    }
                }
                i++;
            }
        }
        return this;
    }

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


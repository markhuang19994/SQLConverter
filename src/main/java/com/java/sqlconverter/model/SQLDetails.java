package com.java.sqlconverter.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class SQLDetails {
    private List<String> upsertTextBlocks = new ArrayList<>();
    private String primaryKey;
    private String sqlFileText;

    public SQLDetails(String sqlFileText) {
        this.sqlFileText = sqlFileText;
        analyzeTextAndSetPrimaryKey(sqlFileText);
        analyzeTextAndSetSqlTexts(sqlFileText);
    }

    private void analyzeTextAndSetPrimaryKey(String sqlFileText) {
        Pattern p = Pattern.compile("--@\\s*pk\\s*:\\s*(.*?)\\s");
        Matcher m = p.matcher(sqlFileText);
        if (m.find()) {
            this.primaryKey = m.group(1);
        }
    }

    private void analyzeTextAndSetSqlTexts(String sqlFileText) {
        String on = "--@upsert:on";
        String off = "--@upsert:off";
        String newSqlFileText = sqlFileText;
        newSqlFileText = newSqlFileText.replaceAll("--@\\s*upsert\\s*:\\s*on", on);
        newSqlFileText = newSqlFileText.replaceAll("--@\\s*upsert\\s*:\\s*off", off);

        int i, j;
        while ((i = newSqlFileText.indexOf(on)) != -1) {
            newSqlFileText = newSqlFileText.substring(i + on.length());
            if ((j = newSqlFileText.indexOf(off)) != -1) {
                upsertTextBlocks.add(newSqlFileText.substring(0, j));
            } else {
                upsertTextBlocks.add(newSqlFileText);
                break;
            }
        }
    }

    public List<String> getUpsertTextBlocks() {
        return upsertTextBlocks;
    }

    public void setUpsertTextBlocks(List<String> upsertTextBlocks) {
        this.upsertTextBlocks = upsertTextBlocks;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getSqlFileText() {
        return sqlFileText;
    }

    public void setSqlFileText(String sqlFileText) {
        this.sqlFileText = sqlFileText;
    }
}

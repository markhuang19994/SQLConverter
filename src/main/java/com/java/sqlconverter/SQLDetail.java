package com.java.sqlconverter;

import java.util.ArrayList;
import java.util.Arrays;
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
public class SQLDetail {
    private final List<String> upsertBlocks;
    private final List<String> primaryKeys;
    private final String       sqlFileText;
    
    public SQLDetail(String sqlFileText) {
        this.sqlFileText = sqlFileText;
        this.primaryKeys = getPrimaryKeys(sqlFileText);
        this.upsertBlocks = getUpsertBlocks(sqlFileText);
    }
    
    private List<String> getPrimaryKeys(String sqlFileText) {
        Pattern p = Pattern.compile("--@pk:[ \t]*(.*)[ \t]*");
        Matcher m = p.matcher(sqlFileText);
        if (m.find()) {
            return Arrays.asList(m.group(1).split(","));
        }
        throw new RuntimeException("--@pk parse fail.");
    }
    
    private List<String> getUpsertBlocks(String sqlFileText) {
        final List<String> upsertTextBlocks = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        final String[] split = sqlFileText.split("\n");
        boolean inUpsertBlock = false;
        int lastUpsertOnIndex = -1;
        for (int i = 1; i <= split.length; i++) {
            final String line = split[i - 1];
            if (line.matches("^--@upsert:on[ \t]*")) {
                if (inUpsertBlock) {
                    throw new RuntimeException(String.format("find @upsert:on in @upsert:on, at line:%s and line:%s", lastUpsertOnIndex, i));
                }
                lastUpsertOnIndex = i;
                inUpsertBlock = true;
            } else if (line.matches("^--@upsert:off[ \t]*")) {
                if (!inUpsertBlock) {
                    throw new RuntimeException("@upsert:on is not declare, bu find @upsert:off at line:" + i);
                }
                upsertTextBlocks.add(sb.toString());
                sb.setLength(0);
                inUpsertBlock = false;
            } else {
                if (inUpsertBlock) {
                    sb.append(line).append("\n");
                }
            }
        }
        
        if (inUpsertBlock) {
            throw new RuntimeException(String.format("@upsert:on is declare at line:%s, but not close(@upsert:off).", lastUpsertOnIndex));
        }
        return upsertTextBlocks;
    }
    
    public List<String> getUpsertBlocks() {
        return upsertBlocks;
    }
    
    public List<String> getPrimaryKeys() {
        return new ArrayList<>(this.primaryKeys);
    }
    
    public String getSqlFileText() {
        return sqlFileText;
    }
    
}

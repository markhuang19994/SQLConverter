package com.java.sqlconverter.converter.impl;

import com.java.sqlconverter.model.SQLDetails;
import com.java.sqlconverter.util.SQLUtil;
import com.java.sqlconverter.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class InsertConverterImpl extends com.java.sqlconverter.converter.impl.InsertAndUpdateConverterImpl {
    
    private static final String UPDATE_SQL_TEMPLATE = "UPDATE {tableName} SET {colKeyVal} {limitedCondition};";
    private static final String INSERT_SQL_TEMPLATE = "INSERT INTO {tableName} ({keys}) VALUES ({values});";
    private static final String UPSERT_TEMPLATE     = "{updateSql}\nIF @@ROWCOUNT=0\n\t{insert}";
    
    public InsertConverterImpl(SQLDetails sqlDetails) {
        this.sqlDetails = sqlDetails;
    }
    
    private String convert2Update() {
        String newSqlFileText = this.sqlDetails.getSqlFileText();
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String sqlText : this.sqlDetails.getUpsertTextBlocks()) {
            List<InsertModel> insertModels = parseInsert(sqlText);
            for (InsertModel insertModel : insertModels) {
                String temp = genUpdateStatement(insertModel);
                sb.append(temp).append("\n");
            }
            int len = sb.length();
            sb.delete(len - 1, len);
            newSqlFileText = newSqlFileText.replace(sqlText, sb.toString());
        }
        return newSqlFileText;
    }
    
    /**
     * 將sql檔案有包含在upsert:on與upsert:off之間的insert轉換成update + insert
     *
     * @return 新的sql檔內容
     */
    @Override
    public String convert2Upsert() {
        String newSqlFileText = this.sqlDetails.getSqlFileText();
        final List<String> upsertTextBlocks = this.sqlDetails.getUpsertTextBlocks();
        for (String sqlText : upsertTextBlocks) {
            List<InsertModel> insertModels = parseInsert(sqlText);
            final StringBuilder upsertSb = new StringBuilder("\r\n");
            for (InsertModel insertModel : insertModels) {
                String updateSql = genUpdateStatement(insertModel);
                String uTemplate = UPSERT_TEMPLATE;
                uTemplate = uTemplate.replace("{updateSql}", updateSql);
                uTemplate = uTemplate.replace("{insert}", genInsertStatement(insertModel));
                upsertSb.append(uTemplate).append("\r\n");
            }
            newSqlFileText = newSqlFileText.replace(sqlText, upsertSb.toString());
            upsertSb.setLength(0);
        }
        return newSqlFileText;
    }
    
    private String genUpdateStatement(InsertModel sql) {
        String temp;
        String insertSql = sql.sqlStr;
        String tableName = sql.tableName;
        String[] keys = sql.keys;
        String[] vals = sql.vals;
        checkKeyValueLength(insertSql, keys, vals);
        temp = UPDATE_SQL_TEMPLATE;
        temp = temp.replace("{tableName}", tableName);
        temp = temp.replace("{colKeyVal}", generateColumnKeyVal(keys, vals));
        temp = temp.replace("{limitedCondition}", checkAndGeneratePrimaryKeyVal(insertSql, keys, vals));
        return temp;
    }
    
    private String genInsertStatement(InsertModel sql) {
        String temp;
        String insertSql = sql.sqlStr;
        String tableName = sql.tableName;
        String[] keys = sql.keys;
        String[] vals = sql.vals;
        checkKeyValueLength(insertSql, keys, vals);
        temp = INSERT_SQL_TEMPLATE;
        temp = temp.replace("{tableName}", tableName);
        temp = temp.replace("{keys}", String.join(", ", keys));
        temp = temp.replace("{values}", String.join(", ", vals));
        return temp;
    }
    
    private String generateColumnKeyVal(String[] keys, String[] vals) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.length; i++)
            sb.append(keys[i]).append(" = ").append(vals[i]).append(", ");
        int len = sb.length();
        sb.delete(len - 2, len);
        return sb.toString();
    }
    
    private String checkAndGeneratePrimaryKeyVal(String insertSql, String[] keys, String[] vals) {
        StringBuilder sb = new StringBuilder();
        String limitKey = "WHERE";
        List<String> pks = this.sqlDetails.getPrimaryKeys();
        for (int i = 0; i < keys.length; i++) {
            if (pks.contains(keys[i].replaceAll("[\\[\\]]", ""))) {
                sb.append(String.format(" %s %s = %s", limitKey, keys[i], vals[i]));
                if (limitKey.equals("WHERE")) {
                    limitKey = "AND";
                }
                pks.remove(keys[i]);
            }
        }
        if (pks.size() == 0) {
            return sb.toString();
        }
        throw new IllegalArgumentException(
                String.format("Insert sqlDetails:%s , 未包含主鍵:%s",
                        StringUtil.abbreviateString(insertSql, 100), pks)
        );
    }
    
    private void checkKeyValueLength(String insertSql, String[] keys, String[] vals) {
        if (keys.length != vals.length) {
            throw new IllegalArgumentException(
                    String.format("Insert sqlDetails:%s ,key的長度不等於value的長度, key:%s, value:%s",
                            StringUtil.abbreviateString(insertSql, 500),
                            Arrays.asList(StringUtil.abbreviateString(keys, 100)),
                            Arrays.asList(StringUtil.abbreviateString(vals, 100)))
            );
        }
    }
    
    /**
     * 取得字串中的insert sql資訊,其中在雙引號中間的逗號(,)會先被轉換成其他值,取得key value後會再轉回來
     * 1.insert格式 :insert [into] tableName (xxx,xxx,xxx) value (ooo,ooo,ooo);, )後如果沒有分號,就必須要有\nGO,
     * 或者是)\n後接INSERT或者)\n為最後一行(\n後皆為空白)
     * 其中insert into value皆不區分大小寫
     *
     * @param sql sqlText
     * @return List<InsertModel>
     */
    private List<InsertModel> parseInsert(String sql) {
        final List<String> insertList = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        
        String topRemain = "";
        boolean isFirst = true;
        for (String line : sql.split("\n")) {
            if (line.matches("^(?i)insert[\\s\\S]*$")) {
                if (sb.length() > 0) {
                    final String str = sb.toString();
                    if (isFirst) {
                        topRemain = str;
                        isFirst = false;
                    } else {
                        insertList.add(str);
                    }
                    sb.setLength(0);
                }
            }
            
            sb.append(line).append("\n");
        }
        
        if (sb.length() > 0) {
            insertList.add(sb.toString());
            sb.setLength(0);
        }
        
        if (topRemain.length() > 0 && !topRemain.equals("\n")) {
//            System.out.println("skip top remain:" + topRemain);
        }
        
        final List<String> errorMsg = new ArrayList<>();
        final List<InsertModel> insertModels = new ArrayList<>();
        final StringBuilder insertSb = new StringBuilder();
        
        for (String insertStmt : insertList) {
            insertStmt = SQLUtil.replaceStatementSensitiveWord(insertStmt);
            insertSb.setLength(0);
            String tableName;
            String[] keys;
            String[] values;
            
            int idx;
            
            idx = insertStmt.toLowerCase().indexOf("insert");
            if (idx == -1) {
                errorMsg.add("parse error, insert key word not found:" + insertStmt);
                continue;
            } else {
                idx = idx + "insert".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf("into");
            if (idx != -1) {
                idx = idx + "into".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf("(");
            if (idx == -1) {
                errorMsg.add("parse error, keys left ( not found:" + insertStmt);
                continue;
            } else {
                tableName = removeBrackets(insertStmt.substring(0, idx).trim());
                idx = idx + "(".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf(")");
            if (idx == -1) {
                errorMsg.add("parse error, keys right ) not found:" + insertStmt);
                continue;
            } else {
                final String keysStr = insertStmt.substring(0, idx).trim();
                keys = Arrays.stream(keysStr.split(","))
                             .map(String::trim)
                             .map(this::removeBrackets).collect(Collectors.toList()).toArray(String[]::new);
                idx = idx + ")".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf("values");
            if (idx == -1) {
                errorMsg.add("parse error, values key word not found:" + insertStmt);
                continue;
            } else {
                idx = idx + "values".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf("(");
            if (idx == -1) {
                errorMsg.add("parse error, values left ( not found:" + insertStmt);
                continue;
            } else {
                idx = idx + "(".length();
                insertSb.append(insertStmt, 0, idx);
                insertStmt = insertStmt.substring(idx);
            }
            
            idx = insertStmt.toLowerCase().indexOf(")");
            if (idx == -1) {
                errorMsg.add("parse error, values right ) not found:" + insertStmt);
                continue;
            } else {
                final String valuesStr = insertStmt.substring(0, idx).trim();
                values = Arrays.stream(valuesStr.split(","))
                               .map(String::trim)
                               .map(this::removeBrackets).collect(Collectors.toList()).toArray(String[]::new);
                idx = idx + ")".length();
                insertSb.append(insertStmt, 0, idx);
            }
            final String upsertStmt = insertSb.toString() + ";";
            final String convertedUpsertStmt = SQLUtil.recoverStatementSensitiveWord(upsertStmt);
            final String remain = insertStmt.substring(idx);
            if (remain.length() > 0 && !remain.equals(";\n")) {
//                System.out.println("skip remain:" + remain);
            }
            insertModels.add(new InsertModel(convertedUpsertStmt, tableName, keys, values));
        }
        
        if (errorMsg.size() > 0) {
            throw new RuntimeException(String.join("\n", errorMsg));
        }
        return insertModels;
    }
    
    private String removeBrackets(String str) {
        if (str.matches("^\\[[\\s\\S]*]$")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
    
    private static class InsertModel {
        private final String   sqlStr;
        private final String   tableName;
        private final String[] keys;
        private final String[] vals;
        
        InsertModel(String sqlStr, String tableName, String[] keys, String[] vals) {
            this.sqlStr = sqlStr;
            this.tableName = tableName;
            this.keys = keys;
            this.vals = vals;
        }
    }
}

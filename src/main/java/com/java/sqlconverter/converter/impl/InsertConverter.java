package com.java.sqlconverter.converter.impl;

import com.java.sqlconverter.SQLDetail;
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
public class InsertConverter {
    
    private static final String    UPDATE_SQL_TEMPLATE = "UPDATE {tableName} SET {colKeyVal} {limitedCondition}";
    private static final String    INSERT_SQL_TEMPLATE = "INSERT INTO {tableName} ({keys}) VALUES ({values})";
    private static final String    UPSERT_TEMPLATE     = "{updateSql}\nIF @@ROWCOUNT=0\n\t{insert}";
    private final        SQLDetail sqlDetail;
    
    
    public InsertConverter(SQLDetail sqlDetail) {
        this.sqlDetail = sqlDetail;
    }

//    private String convert2Update() {
//        String newSqlFileText = this.sqlDetails.getSqlFileText();
//        StringBuilder sb = new StringBuilder();
//        sb.append("\n");
//        for (String sqlText : this.sqlDetails.getUpsertTextBlocks()) {
//            List<InsertModel> insertModels = parseInsert(sqlText);
//            for (InsertModel insertModel : insertModels) {
//                String temp = genUpdateStatement(insertModel);
//                sb.append(temp).append("\n");
//            }
//            int len = sb.length();
//            sb.delete(len - 1, len);
//            newSqlFileText = newSqlFileText.replace(sqlText, sb.toString());
//        }
//        return newSqlFileText;
//    }
    
    /**
     * 將sql檔案有包含在upsert:on與upsert:off之間的insert轉換成update + insert
     *
     * @return 新的sql檔內容
     */
    public String convert2Upsert() {
        String newSqlFileText = this.sqlDetail.getSqlFileText();
        final List<String> upsertTextBlocks = this.sqlDetail.getUpsertBlocks();
        for (int i = 1; i <= upsertTextBlocks.size(); i++) {
            final String sqlText = upsertTextBlocks.get(i - 1);
            final List<InsertModel> insertModels = parseInsert(sqlText, i);
            final StringBuilder upsertSb = new StringBuilder();
            for (InsertModel insertModel : insertModels) {
                checkKeyValueLength(insertModel);
                String updateSql = genUpdateStatement(insertModel);
                String uTemplate = UPSERT_TEMPLATE;
                uTemplate = uTemplate.replace("{updateSql}", updateSql);
                uTemplate = uTemplate.replace("{insert}", genInsertStatement(insertModel) + ";");
                upsertSb.append(uTemplate).append("\r\n\r\n");
            }
            newSqlFileText = newSqlFileText.replace(sqlText, upsertSb.toString());
            upsertSb.setLength(0);
        }
        return newSqlFileText;
    }
    
    private String genUpdateStatement(InsertModel insertModel) {
        String tableName = insertModel.tableName;
        String[] keys = insertModel.keys;
        String[] vals = insertModel.vals;
        String temp = UPDATE_SQL_TEMPLATE;
        temp = temp.replace("{tableName}", tableName);
        temp = temp.replace("{colKeyVal}", generateColumnKeyVal(keys, vals));
        temp = temp.replace("{limitedCondition}", checkAndGeneratePrimaryKeyVal(insertModel));
        return temp;
    }
    
    private String genInsertStatement(InsertModel sql) {
        String tableName = sql.tableName;
        String[] keys = sql.keys;
        String[] vals = sql.vals;
        String temp = INSERT_SQL_TEMPLATE;
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
    
    private String checkAndGeneratePrimaryKeyVal(InsertModel insertModel) {
        String insertSql = insertModel.sqlStr;
        String[] keys = insertModel.keys;
        String[] vals = insertModel.vals;
        StringBuilder sb = new StringBuilder();
        String limitKey = "WHERE";
        List<String> pks = this.sqlDetail.getPrimaryKeys();
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
                String.format("at upsert:%s,line:%s >> %s , 未包含主鍵:%s",
                        insertModel.atBlock,
                        insertModel.atLine,
                        StringUtil.abbreviateString(insertSql, 100), pks
                )
        );
    }
    
    private void checkKeyValueLength(InsertModel insertModel) {
        String insertSql = insertModel.sqlStr;
        String[] keys = insertModel.keys;
        String[] vals = insertModel.vals;
        if (keys.length != vals.length) {
            throw new IllegalArgumentException(
                    String.format("at upsert:%s,line:%s >> %s ,key的長度不等於value的長度, key:%s, value:%s",
                            insertModel.atBlock,
                            insertModel.atLine,
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
    private List<InsertModel> parseInsert(String sql, int atBlock) {
        final long l = System.currentTimeMillis();
        final List<InsertStmt> insertList = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        
        String topRemain = "";
        boolean isFirst = true;
        
        final String[] lines = sql.split("\n");
        int currentInsertStartLine = -1;
        for (int i = 1; i <= lines.length; i++) {
            final String line = lines[i - 1];
            
            if (line.matches("^(?i)insert.*$")) {
                currentInsertStartLine = i;
                final String str = sb.toString();
                if (isFirst) {
                    topRemain = str;
                    isFirst = false;
                } else {
                    insertList.add(new InsertStmt(str, currentInsertStartLine));
                }
                sb.setLength(0);
            }
            
            sb.append(line).append("\n");
        }
        
        if (sb.length() > 0) {
            insertList.add(new InsertStmt(sb.toString(), currentInsertStartLine));
            sb.setLength(0);
        }
        
        if (topRemain.length() > 0 && !topRemain.equals("\n")) {
//            System.out.println("skip top remain:" + topRemain);
        }
        
        final List<String> errorMsg = new ArrayList<>();
        final List<InsertModel> insertModels = new ArrayList<>();
        final StringBuilder insertSb = new StringBuilder();
        
        for (InsertStmt insertStmt : insertList) {
            String stmt = insertStmt.stmt;
            stmt = SQLUtil.replaceStatementSensitiveWord(stmt);
            insertSb.setLength(0);
            String tableName;
            String[] keys;
            String[] values;
            
            int idx;
            
            idx = stmt.toLowerCase().indexOf("insert");
            if (idx == -1) {
                errorMsg.add("parse error, insert key word not found:" + stmt);
                continue;
            } else {
                idx = idx + "insert".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf("into");
            if (idx != -1) {
                idx = idx + "into".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf("(");
            if (idx == -1) {
                errorMsg.add("parse error, keys left ( not found:" + stmt);
                continue;
            } else {
                tableName = removeBrackets(stmt.substring(0, idx).trim());
                idx = idx + "(".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf(")");
            if (idx == -1) {
                errorMsg.add("parse error, keys right ) not found:" + stmt);
                continue;
            } else {
                final String keysStr = stmt.substring(0, idx).trim();
                keys = Arrays.stream(keysStr.split(","))
                             .map(String::trim)
                             .map(this::removeBrackets).collect(Collectors.toList()).toArray(String[]::new);
                idx = idx + ")".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf("values");
            if (idx == -1) {
                errorMsg.add("parse error, values key word not found:" + stmt);
                continue;
            } else {
                idx = idx + "values".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf("(");
            if (idx == -1) {
                errorMsg.add("parse error, values left ( not found:" + stmt);
                continue;
            } else {
                idx = idx + "(".length();
                insertSb.append(stmt, 0, idx);
                stmt = stmt.substring(idx);
            }
            
            idx = stmt.toLowerCase().indexOf(")");
            if (idx == -1) {
                errorMsg.add("parse error, values right ) not found:" + stmt);
                continue;
            } else {
                final String valuesStr = stmt.substring(0, idx).trim();
                values = Arrays.stream(valuesStr.split(","))
                               .map(String::trim)
                               .map(this::removeBrackets).collect(Collectors.toList()).toArray(String[]::new);
                idx = idx + ")".length();
                insertSb.append(stmt, 0, idx);
            }
            final String cleanInsertStmt = SQLUtil.recoverStatementSensitiveWord(insertSb.toString());
            final String remain = stmt.substring(idx);
            if (remain.length() > 0 && !remain.equals(";\n")) {
//                System.out.println("skip remain:" + remain);
            }
            insertModels.add(new InsertModel(insertSb.toString(), atBlock, insertStmt.atLine, tableName, keys, values));
        }
        
        if (errorMsg.size() > 0) {
            throw new RuntimeException(String.join("\n", errorMsg));
        }
        System.out.println("ct:" + (System.currentTimeMillis() -l));
        return insertModels;
    }
    
    private String removeBrackets(String str) {
        final List<String> l = new ArrayList<>();
        for (String s : str.split("\\.")) {
            if (s.matches("^\\[[\\s\\S]*]$")) {
                l.add(s.substring(1, s.length() - 1));
            } else {
                l.add(s);
            }
        }
        return String.join(".", l);
    }
    
    private static class InsertStmt {
        private final String stmt;
        private final int    atLine;
        
        private InsertStmt(String stmt, int atLine) {
            this.stmt = stmt;
            this.atLine = atLine;
        }
    }
    
    private static class InsertModel {
        private final String   sqlStr;
        private final int      atBlock;
        private final int      atLine;
        private final String   tableName;
        private final String[] keys;
        private final String[] vals;
        
        InsertModel(String sqlStr, int atBlock, int atLine, String tableName, String[] keys, String[] vals) {
            this.sqlStr = sqlStr;
            this.atBlock = atBlock;
            this.atLine = atLine;
            this.tableName = tableName;
            this.keys = keys;
            this.vals = vals;
        }
    }
}

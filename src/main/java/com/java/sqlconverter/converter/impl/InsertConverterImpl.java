package com.java.sqlconverter.converter.impl;

import com.java.sqlconverter.model.SQLDetails;
import com.java.sqlconverter.util.StringUtil;

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
public class InsertConverterImpl extends com.java.sqlconverter.converter.impl.InsertAndUpdateConverterImpl {

    private static final String UPDATE_SQL_TEMPLATE = "UPDATE {tableName} SET {colKeyVal} {limitedCondition}";
    private static final String UPSERT_TEMPLATE = "{updateSql}\nIF @@ROWCOUNT=0\n\t{insert}";
    private static final String DUMMY_COMMA = "$OAOOUOA_A$";
    public static final String DUMMY_SEMICOLON = "#dummySemicolon#";

    public InsertConverterImpl(SQLDetails sqlDetails) {
        this.sqlDetails = sqlDetails;
    }

    private String convert2Update() {
        String newSqlFileText = this.sqlDetails.getSqlFileText();
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        for (String sqlText : this.sqlDetails.getUpsertTextBlocks()) {
            List<InsertModel> insertModels = getInsertSqls(sqlText);
            for (InsertModel insertModel : insertModels) {
                String temp = insert2Update(insertModel);
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
        for (String sqlText : this.sqlDetails.getUpsertTextBlocks()) {
            List<InsertModel> insertModels = getInsertSqls(sqlText);
            for (InsertModel insertModel : insertModels) {
                String updateSql = insert2Update(insertModel);
                String uTemplate = UPSERT_TEMPLATE;
                uTemplate = uTemplate.replace("{updateSql}", updateSql);
                uTemplate = uTemplate.replace("{insert}", insertModel.sqlStr);
                newSqlFileText = newSqlFileText.replace(insertModel.sqlStr, uTemplate);
            }
        }
        return newSqlFileText;
    }

    private String insert2Update(InsertModel sql) {
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
                sb.append(String.format("%s %s = %s ", limitKey, keys[i], vals[i]));
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
     * @param sqlText sqlText
     * @return List<InsertModel>
     */
    private List<InsertModel> getInsertSqls(String sqlText) {
        List<InsertModel> result = new ArrayList<>();
        Pattern p = Pattern.compile("((?i)insert)\\s+(((?i)into)\\s+)?(.*?)\\s+\\(([\\s\\S]*?)\\)\\s+((?i)values)\\s+\\(([\\s\\S]*?)\\)((\\s*?;)|\\s+GO|" + DUMMY_SEMICOLON + ")");
        //比較嚴格的 pattern,最後)後方必須直接接;或者下一行開頭是GO
        Pattern strictP = Pattern.compile("((?i)insert)\\s+(((?i)into)\\s+)?(.*?)\\s+\\(([\\s\\S]*?)\\)\\s+((?i)values)\\s+\\(([\\s\\S]*?)\\)(;|\nGO)");
        Matcher m = p.matcher(sqlText);
        while (m.find()) {
            String insertSql = m.group(0);
            if (insertSql.charAt(insertSql.length() - 1) == '\n') {
                insertSql = insertSql.substring(0, insertSql.length() - 1);
            }
            String tableName = m.group(4);
            String[] keys = replaceCommaInSingleQuotation(m.group(5)).split(",");
            keys = StringUtil.wordsReplace(DUMMY_COMMA, ",", keys);

            String[] vals = m.group(7).split(",");

            InsertModel insertModel = new InsertModel(insertSql, tableName, trimStringArray(keys), trimStringArray(vals));
            result.add(insertModel);
        }
        return result;
    }

    private String[] trimStringArray(String[] arr) {
        String[] newArr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            newArr[i] = arr[i].trim();
        }
        return newArr;
    }

    private String replaceCommaInSingleQuotation(String str) {
        char[] chars = str.toCharArray();
        boolean isFindBeginQuotation = false;
        boolean isEscape = false;
        StringBuilder sb = new StringBuilder();
        for (int index = 0; index < chars.length; index++) {
            char c = chars[index];
            if (isEscape) {
                sb.append(c);
                isEscape = false;
                continue;
            }

            if (c == '\\') {
                isEscape = true;
            } else if (c == '\'') {
                if (isFindBeginQuotation && index + 1 < chars.length && chars[index + 1] == '\'') {//如果在字串裡面的"''"就當跳脫字元
                    isEscape = true;
                } else {
                    isFindBeginQuotation = !isFindBeginQuotation;
                }
            }

            if (c == ',' && isFindBeginQuotation) {
                sb.append(DUMMY_COMMA);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private class InsertModel {
        private String sqlStr;
        private String tableName;
        private String[] keys;
        private String[] vals;

        InsertModel(String sqlStr, String tableName, String[] keys, String[] vals) {
            this.sqlStr = sqlStr;
            this.tableName = tableName;
            this.keys = keys;
            this.vals = vals;
        }
    }
}

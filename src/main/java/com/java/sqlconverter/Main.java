package com.java.sqlconverter;

import com.java.sqlconverter.converter.impl.InsertConverter;
import com.java.sqlconverter.util.FileUtil;
import com.java.sqlconverter.util.SQLUtil;

import java.io.File;
import java.nio.file.Files;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/17, MarkHuang,new
 * </ul>
 * @since 2018/12/17
 */
public class Main {
    
    public static void main(String[] args) {
        try {
            String p = "/home/mark/Desktop/sqlConverter/src/main/resources/test2.sql";
            String sqlText = FileUtil.readFile(p);
            long l = System.currentTimeMillis();
            
            SQLDetail sqlDetail = new SQLDetail(sqlText);
            //2.將insert轉換成update + insert (upsert)
            String newSqlText = new InsertConverter(sqlDetail).convert2Upsert();
            newSqlText = SQLUtil.recoverStatementSensitiveWord(newSqlText);
            newSqlText = SQLUtil.removeUpsertComments(newSqlText);
            Files.write(new File(new File(p).getParent(), "res.sql").toPath(), newSqlText.getBytes());
            
            System.out.println("耗費時間:" + (System.currentTimeMillis() - l) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}





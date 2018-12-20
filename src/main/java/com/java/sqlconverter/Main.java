package com.java.sqlconverter;

import com.java.sqlconverter.constant.ConvertType;
import com.java.sqlconverter.converter.impl.InsertConverterImpl;
import com.java.sqlconverter.factory.InsertAndUpdateConverterFactory;
import com.java.sqlconverter.model.SQLCommentCheckerReport;
import com.java.sqlconverter.model.SQLDetails;
import com.java.sqlconverter.util.SQLUtil;
import com.java.sqlconverter.validate.SQLCommentCheck;
import com.java.sqlconverter.validate.builder.SQLSyntaxCheckBuilder;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/17, MarkHuang,new
 * </ul>
 * @since 2018/12/17
 */
public class Main {
    /**
     * main 方法中有3個步驟,任意步驟出錯都會立即停止
     *
     * @param args args
     */
    public static void main(String[] args) {
        long l = System.currentTimeMillis();
        //1.check sql檔案格式是否正確
        boolean isCorrectSyntax = SQLSyntaxCheckBuilder
                .build()
                .setSqlFileText(SampleSQLText.TEXT_1)
                .setHost("localhost")
                .setPort("1433")
                .setUserName("sa")
                .setPassword("p@ssw0rd")
                .create()
                .check();
        if (!isCorrectSyntax) {
            throw new IllegalArgumentException("SQL syntax is not correct!");
        }

        //2.check 自定義註釋是否正確 如:--{} init等等
        SQLDetails sqlDetails = new SQLDetails(
                SQLUtil.complementDummyInsertSemicolonAndReplaceSensitiveWordsInInsertValues(SampleSQLText.TEXT_1)
        );
        SQLCommentCheck commentChecker = SQLCommentCheck.getInstance();
        SQLCommentCheckerReport report = commentChecker.check(sqlDetails);

        if (report.hasError()) {
            String errorMessage = commentChecker.generateErrorMessage(report.getErrorMap());
            throw new IllegalArgumentException(errorMessage);
        }

        //3.將insert轉換成update + insert (upsert)
        String newSqlFileText = InsertAndUpdateConverterFactory
                .createConverter(sqlDetails, ConvertType.INSERT)
                .convert2Upsert();
        System.out.println(newSqlFileText.replace(InsertConverterImpl.DUMMY_SEMICOLON, ""));
        System.out.println("耗費時間:" + (System.currentTimeMillis() - l) + "ms");
    }
}





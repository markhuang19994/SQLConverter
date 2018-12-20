package com.java.sqlconverter;

import com.java.sqlconverter.constant.ConvertType;
import com.java.sqlconverter.factory.InsertAndUpdateConverterFactory;
import com.java.sqlconverter.model.SQLDetails;
import com.java.sqlconverter.util.CommentCheckReport;
import com.java.sqlconverter.util.SyntaxCheckReport;
import com.java.sqlconverter.validate.CommentRule;
import com.java.sqlconverter.util.SQLUtil;
import com.java.sqlconverter.util.StringUtil;
import com.java.sqlconverter.validate.*;
import com.java.sqlconverter.validate.builder.SQLSyntaxCheckBuilder;

import java.util.*;

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
        try {
            long l = System.currentTimeMillis();
            //1.check sql檔案格式是否正確
            SyntaxCheckReport syntaxCheckReport = SQLSyntaxCheckBuilder
                    .build()
                    .setSqlFileText(SampleSQLText.TEXT_1)
                    .setHost("localhost")
                    .setPort("1433")
                    .setUserName("sa")
                    .setPassword("p@ssw0rd")
                    .create()
                    .check();
            if (!syntaxCheckReport.isSyntaxCorrect()) {
                throw new IllegalArgumentException("SQL syntax is not correct!");
            }

            //2.check 自定義註釋是否正確 如:--{} init等等
            SQLDetails sqlDetails = new SQLDetails(
                    SQLUtil.complementDummyInsertSemicolonAndReplaceSensitiveWordsInInsertValues(SampleSQLText.TEXT_1)
            );
            SQLCommentCheck commentChecker = validateSql();
            List<CommentCheckReport> commentCheckReports = commentChecker.generateCommentAndLine().processCommentRule();

            if (!commentChecker.isAllPass()) {
                String errorMessage = generateErrorMessageFromReports(commentCheckReports, false);
                throw new IllegalArgumentException(errorMessage);
            }

            //3.將insert轉換成update + insert (upsert)
            String newSqlFileText = InsertAndUpdateConverterFactory
                    .createConverter(sqlDetails, ConvertType.INSERT)
                    .convert2Upsert();
            System.out.println(SQLUtil.recoverInsertSql(newSqlFileText));
            System.out.println("耗費時間:" + (System.currentTimeMillis() - l) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static SQLCommentCheck validateSql() {
        SQLCommentCheck check = new SQLCommentCheck(SampleSQLText.TEXT_1);

        check.register(new CommentRule() {
            private final List<String> regexs = Arrays.asList(
                    "--\\{}\\s*?init\\s*?", "--\\{}\\s*?update\\s*?",
                    "--\\{}\\s*?SYS_TYPE\\s*?", "--\\{}\\s*?TRANS_TYPE\\s*?",
                    "--\\{}\\s*?NORMAL_TYPE\\s*?"
            );

            private final String correctFormat = "--{} + init|update|SYS_TYPE|TRANS_TYPE|NORMAL_TYPE";

            @Override
            public CommentCheckReport checkComment(List<CommentAndLine> commentAndLines) {
                List<String> errorMessages = new ArrayList<>();
                for (CommentAndLine commentAndLine : commentAndLines) {
                    String comment = commentAndLine.getComment();
                    int line = commentAndLine.getLine();
                    if (comment.indexOf("--{}") == 0) {
                        boolean isPass = StringUtil.checkStringWithRegex(comment, regexs);
                        if (!isPass) {
                            errorMessages.add(String.format("第%d行:%s格式錯誤,正確格式為:%s",
                                    line, comment, correctFormat
                            ));
                        }
                    }
                }
                errorMessages.addAll(checkRational(commentAndLines));
                return new CommentCheckReport(errorMessages.size() == 0, errorMessages);
            }

            @Override
            public boolean isLineNeedCheck(String line) {
                return line.matches("^(--\\{}.*)$");
            }

            private List<String> checkRational(List<CommentAndLine> commentAndLines) {
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
        });


        check.register(new CommentRule() {
            private final List<String> regexs = Arrays.asList(
                    "--@\\s*?pk\\s*?:\\s*?.+",
                    "--@\\s*?upsert\\s*?:\\s*?(on|off)"
            );

            private final String correctFormat = "--@update:on/off|--@pk:primaryKey";

            @Override
            public CommentCheckReport checkComment(List<CommentAndLine> commentAndLines) {
                List<String> errorMessages = new ArrayList<>();
                for (CommentAndLine commentAndLine : commentAndLines) {
                    String comment = commentAndLine.getComment();
                    int line = commentAndLine.getLine();
                    if (comment.indexOf("--@") == 0) {
                        boolean isPass = StringUtil.checkStringWithRegex(comment, regexs);
                        if (!isPass) {
                            errorMessages.add(String.format("第%d行:%s格式錯誤,正確格式為:%s",
                                    line, comment, correctFormat
                            ));
                        }
                    }
                }
                errorMessages.addAll(checkRational(commentAndLines));
                return new CommentCheckReport(errorMessages.size() == 0, errorMessages);
            }

            @Override
            public boolean isLineNeedCheck(String line) {
                return line.matches("^(--@.*)$");
            }

            private List<String> checkRational(List<CommentAndLine> commentAndLines) {
                List<String> errorMessages = new ArrayList<>();
                int upserOnCount = 0;
                int upserOffCount = 0;
                boolean isPkDeclare = false;
                for (CommentAndLine commentAndLine : commentAndLines) {
                    String comment = commentAndLine.getComment();
                    int line = commentAndLine.getLine();
                    if (comment.contains("pk")) {
                        isPkDeclare = true;
                    } else if (comment.matches("--@\\s*upsert\\s*:\\s*on")) {
                        upserOnCount += 1;
                    } else if (comment.matches("--@\\s*upsert\\s*:\\s*off")) {
                        upserOffCount++;
                        if (upserOnCount < upserOffCount) {
                            errorMessages.add(String.format("第%d行:uppsert on off 順序錯誤", line));
                        }
                    }
                }
                if (upserOnCount > 0 && !isPkDeclare) {
                    errorMessages.add("如果有宣告upsert,應該要在開頭先宣告=>pk:primaryKey");
                }
                return errorMessages;
            }
        });

        return check;
    }

    private static String generateErrorMessageFromReports(List<CommentCheckReport> commentCheckReports, boolean noMatterPass) {
        StringBuilder sb = new StringBuilder("錯誤訊息:").append(System.lineSeparator());
        for (CommentCheckReport commentCheckReport : commentCheckReports) {
            if (noMatterPass || !commentCheckReport.isPass()) {
                List<String> errorMessages = commentCheckReport.getErrorMessages();
                for (String errorMessage : errorMessages) {
                    sb.append(errorMessage).append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }

}





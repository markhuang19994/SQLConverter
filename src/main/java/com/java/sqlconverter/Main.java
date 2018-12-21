package com.java.sqlconverter;

import com.java.sqlconverter.constant.ConvertType;
import com.java.sqlconverter.factory.InsertAndUpdateConverterFactory;
import com.java.sqlconverter.model.SQLDetails;
import com.java.sqlconverter.model.CommentCheckReport;
import com.java.sqlconverter.model.SyntaxCheckReport;
import com.java.sqlconverter.util.FileUtil;
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

    private static String sqlFilePath;
    private static String errorFilePath;


    /**
     * main方法中有3個步驟,任意步驟出錯都會立即停止程序繼續執行,
     * 但呼叫本程式的程序拿到的回傳值永遠為0(正確),即:本程序失敗並不影響其他流程,
     * 如果本程序出錯停止,則會產出一份error file記錄錯誤資訊
     *
     * @param args args
     */
    public static void main(String[] args) {
        try {
            boolean isArgParse = parseArgs(args);
            if (!isArgParse) {
                return;
            }
            String sqlText = FileUtil.readFile(sqlFilePath);
            long l = System.currentTimeMillis();
            //1.check sql檔案格式是否正確
            SyntaxCheckReport syntaxCheckReport = SQLSyntaxCheckBuilder
                    .build()
                    .setSqlFileText(sqlText)
                    .setHost("localhost")
                    .setPort("1433")
                    .setUserName("sa")
                    .setPassword("p@ssw0rd")
                    .create()
                    .check();
            if (!syntaxCheckReport.isSyntaxCorrect()) {
                throw new IllegalArgumentException("SQL syntax is not correct: \n" + syntaxCheckReport.getErrorMessages());
            }

            //2.check 自定義註釋是否正確 如:--{} init等等
            SQLDetails sqlDetails = new SQLDetails(
                    SQLUtil.complementDummyInsertSemicolonAndReplaceSensitiveWordsInInsertValues(sqlText)
            );
            SQLCommentCheck commentChecker = validateSql(sqlText);
            List<CommentCheckReport> commentCheckReports = commentChecker.generateCommentAndLine().processCommentRule();

            if (!commentChecker.isAllPass()) {
                String errorMessage = SQLUtil.generateErrorMessageFromReports(commentCheckReports, false);
                throw new IllegalArgumentException(errorMessage);
            }

            //3.將insert轉換成update + insert (upsert)
            String newSqlFileText = InsertAndUpdateConverterFactory
                    .createConverter(sqlDetails, ConvertType.INSERT)
                    .convert2Upsert();
            System.out.println(SQLUtil.removeUpsertComments(SQLUtil.recoverInsertSql(newSqlFileText)));
            System.out.println("耗費時間:" + (System.currentTimeMillis() - l) + "ms");
        } catch (Exception e) {
            if (errorFilePath != null) {
                FileUtil.writeFile(errorFilePath, e.getMessage());
            }
            e.printStackTrace();
        }
    }

    /**
     * 初始化需要檢驗的註釋
     *
     * @param sqlText sqlText
     * @return SQLCommentCheck
     */
    private static SQLCommentCheck validateSql(String sqlText) {
        SQLCommentCheck check = new SQLCommentCheck(sqlText);

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
                        boolean isPass = StringUtil.checkStringMatchRegexs(comment, regexs);
                        if (!isPass) {
                            errorMessages.add(String.format("第%d行:%s格式錯誤,正確格式為:%s",
                                    line, comment, correctFormat
                            ));
                        }
                    }
                }
                errorMessages.addAll(checkLogicRational(commentAndLines));
                return new CommentCheckReport(errorMessages.size() == 0, errorMessages);
            }

            @Override
            public boolean isLineNeedCheck(String line) {
                return line.matches("^(--\\{}.*)$");
            }

            private List<String> checkLogicRational(List<CommentAndLine> commentAndLines) {
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
                        boolean isPass = StringUtil.checkStringMatchRegexs(comment, regexs);
                        if (!isPass) {
                            errorMessages.add(String.format("第%d行:%s格式錯誤,正確格式為:%s",
                                    line, comment, correctFormat
                            ));
                        }
                    }
                }
                errorMessages.addAll(checkLogicRational(commentAndLines));
                return new CommentCheckReport(errorMessages.size() == 0, errorMessages);
            }

            @Override
            public boolean isLineNeedCheck(String line) {
                return line.matches("^(--@.*)$");
            }

            private List<String> checkLogicRational(List<CommentAndLine> commentAndLines) {
                List<String> errorMessages = new ArrayList<>();
                int upsertOnCount = 0;
                int upsertOffCount = 0;
                boolean isPkDeclare = false;
                for (CommentAndLine commentAndLine : commentAndLines) {
                    String comment = commentAndLine.getComment();
                    int line = commentAndLine.getLine();
                    if (comment.contains("pk")) {
                        isPkDeclare = true;
                    } else if (comment.matches("--@\\s*upsert\\s*:\\s*on")) {
                        upsertOnCount += 1;
                    } else if (comment.matches("--@\\s*upsert\\s*:\\s*off")) {
                        upsertOffCount++;
                        if (upsertOnCount < upsertOffCount) {
                            errorMessages.add(String.format("第%d行:uppsert:off之前要先宣告upsert:on", line));
                        }
                    }
                }
                if (upsertOnCount > 0 && !isPkDeclare) {
                    errorMessages.add("如果有宣告upsert,應該要在開頭先宣告=>pk:primaryKey");
                }
                return errorMessages;
            }
        });

        return check;
    }

    private static boolean parseArgs(String[] args) {
        int i = 0;

        while (i < args.length) {
            if ((i + 1) >= args.length) {
                usage();
                return false;
            }
            if (args[i].compareTo("--sql.file.path") == 0 || args[i].compareTo("-sfp") == 0) {
                sqlFilePath = args[i + 1];
                i += 2;
            } else if (args[i].compareTo("--error.file.path") == 0 || args[i].compareTo("-efp") == 0) {
                errorFilePath = args[i + 1];
                i += 2;
            } else {
                System.err.println("Unrecognized parameter: " + args[i]);
                usage();
                return false;
            }
        }
        return true;
    }

    private static void usage() {
        System.err.println("\nUsage: sql converter [options]");
        System.err.println();
        System.err.println("\twhere options are:");
        System.err.println("\t-sfp \n\t\t--sql.file.path [your sql file path] : The sql file to be convert");
        System.err.println("\t-efp \n\t\t--error.file.path [your error file path] : The error file, when convert fail generate");
        System.err.println();
    }

}





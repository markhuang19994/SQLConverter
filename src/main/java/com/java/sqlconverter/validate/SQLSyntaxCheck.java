package com.java.sqlconverter.validate;

import com.java.sqlconverter.model.SyntaxCheckReport;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * 使用sqlcmd檢查sql格式是否正確,如果正確將不會顯示任何訊息,
 * 並回傳true
 *
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class SQLSyntaxCheck {

    private String host;
    private String port;
    private String userName;
    private String password;
    private String database;
    private String sqlFileText;

    public SQLSyntaxCheck(String host, String port, String userName,
                          String password, String sqlFileText) {
        this.host = host;
        this.port = port;
        this.userName = userName;
        this.password = password;
        this.sqlFileText = sqlFileText;
    }

    public SyntaxCheckReport check() {
        File tempFile = createTempFile(
                String.valueOf(System.currentTimeMillis()), ".sql",
                "SET NOEXEC ON;\n" + this.sqlFileText + "\nSET NOEXEC OFF;"
        );
        ProcessBuilder pb = new ProcessBuilder(generateComment(tempFile.getAbsolutePath()));
        pb.directory(tempFile.getParentFile());

        try {
            Process process = pb.start();
            String[] console = processConsole(process);
            int exitCode = process.waitFor();
            //如果sqlcmd呼叫正確exitCode=0,如果sql語法都正確回傳""
            return new SyntaxCheckReport(
                    exitCode == 0 && "".equals(console[1])
                    , console[0], console[1]
            );
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Process sqlcmd error" + e.getMessage());
        } finally {
            tempFile.deleteOnExit();
        }
    }

    private List<String> generateComment(String temFilePath) {
        List<String> result = new ArrayList<>();
        result.add("sqlcmd");
        if (this.host != null) {
            result.add("-S");
            if (this.port != null) {
                result.add(String.format("%s,%s", host, port));
            } else {
                result.add(this.host);
            }
        }
        if (this.userName != null) {
            result.add("-U");
            result.add(this.userName);
        }
        if (this.password != null) {
            result.add("-P");
            result.add(this.password);
        }
        if (this.database != null) {
            result.add("-d");
            result.add(this.database);
        }
        result.add("-i");
        result.add(temFilePath);
        return result;
    }

    private String[] processConsole(Process process) {
        String[] result = new String[]{"", ""};

        try (Scanner reader = new Scanner(process.getInputStream(), "big5");
             Scanner errorReader = new Scanner(process.getErrorStream(), "big5")) {

            result[0] = readConsole(reader);
            result[1] = readConsole(errorReader);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private String readConsole(Scanner sca) {
        StringBuilder builder = new StringBuilder();
        while (sca.hasNextLine()) {
            builder.append(sca.nextLine());
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    private File createTempFile(String prefix, String suffix, String text) {
        File f;
        try {
            f = File.createTempFile(prefix, suffix);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)));
            bw.write(text);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("can't write temp sql file" + e.getMessage());
        }
        return f;
    }


}

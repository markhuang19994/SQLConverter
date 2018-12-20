package com.java.sqlconverter.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Scanner;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class FileUtil {
    public static String readFile(String path) {
        try (Scanner sca = new Scanner(new File(path))) {
            StringBuilder sb = new StringBuilder();
            while (sca.hasNextLine()) {
                sb.append(sca.nextLine()).append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static void writeFile(String path, String... contents) {
        try (PrintWriter pw = new PrintWriter(new File(path))) {
            for (String content : contents) {
                pw.write(content + System.lineSeparator());
            }
            pw.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(String path, List<String> contents) {
        writeFile(path, contents.toArray(new String[]{}));
    }
}

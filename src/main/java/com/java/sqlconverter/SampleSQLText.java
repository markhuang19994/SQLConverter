package com.java.sqlconverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
class SampleSQLText {

    public static final String TEXT_1 = readFile("C:\\Users\\1710002NB01\\Documents\\GIT\\SQLConverter\\src\\main\\resources\\sql.txt");


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
}

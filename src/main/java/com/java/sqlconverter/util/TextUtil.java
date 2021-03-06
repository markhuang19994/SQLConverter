package com.java.sqlconverter.util;

/**
 * @author Mark Huang
 * @since 5/19/21
 */
public class TextUtil {
    public static final String WINDOWS_SEPARATOR = "\r\n";
    public static final String LINUX_SEPARATOR   = "\n";
    
    public static String toWindowsStyle(String text) {
        return text.replace(LINUX_SEPARATOR, WINDOWS_SEPARATOR);
    }
    
    public static String toLinuxStyle(String text) {
        return text.replace(WINDOWS_SEPARATOR, LINUX_SEPARATOR);
    }
}

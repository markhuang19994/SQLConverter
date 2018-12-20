package com.java.sqlconverter.validate;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class CommentAndLine {
    private String comment;
    private int line;

    public CommentAndLine(String comment, int line) {
        this.comment = comment;
        this.line = line;
    }

    public String getComment() {
        return comment;
    }

    public int getLine() {
        return line;
    }
}

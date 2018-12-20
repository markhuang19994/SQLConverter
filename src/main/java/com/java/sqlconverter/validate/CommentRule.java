package com.java.sqlconverter.validate;

import com.java.sqlconverter.util.CommentCheckReport;
import com.java.sqlconverter.validate.CommentAndLine;

import java.util.List;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public interface CommentRule {
    CommentCheckReport checkComment(List<CommentAndLine> commentAndLines);

    boolean isLineNeedCheck(String line);
}

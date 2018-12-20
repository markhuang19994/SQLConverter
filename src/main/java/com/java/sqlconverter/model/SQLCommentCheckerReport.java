package com.java.sqlconverter.model;

import java.util.List;
import java.util.Map;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class SQLCommentCheckerReport {

    private boolean hasError;
    private Map<String, List<Integer>> errorMap;

    public SQLCommentCheckerReport(boolean hasError, Map<String, List<Integer>> errorMap) {
        this.hasError = hasError;
        this.errorMap = errorMap;
    }

    public boolean hasError() {
        return hasError;
    }

    public Map<String, List<Integer>> getErrorMap() {
        return errorMap;
    }
}

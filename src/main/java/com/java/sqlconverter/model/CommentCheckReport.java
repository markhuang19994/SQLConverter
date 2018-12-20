package com.java.sqlconverter.model;

import java.util.List;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class CommentCheckReport {
    private boolean isPass;
    private List<String> errorMessages;

    public CommentCheckReport(boolean isPass, List<String> errorMessages) {
        this.isPass = isPass;
        this.errorMessages = errorMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean isPass() {
        return isPass;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void setPass(boolean pass) {
        isPass = pass;
    }
}

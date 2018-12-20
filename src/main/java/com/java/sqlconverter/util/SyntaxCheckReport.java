package com.java.sqlconverter.util;

import java.util.List;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class SyntaxCheckReport {
    private boolean isSyntaxCorrect;
    private List<String> errorMessages;

    public SyntaxCheckReport(boolean isSyntaxCorrect, List<String> errorMessages) {
        this.isSyntaxCorrect = isSyntaxCorrect;
        this.errorMessages = errorMessages;
    }

    public List<String> getErrorMessages() {
        return errorMessages;
    }

    public boolean isSyntaxCorrect() {
        return isSyntaxCorrect;
    }

    public void setErrorMessages(List<String> errorMessages) {
        this.errorMessages = errorMessages;
    }

    public void setSyntaxCorrect(boolean syntaxCorrect) {
        isSyntaxCorrect = syntaxCorrect;
    }
}

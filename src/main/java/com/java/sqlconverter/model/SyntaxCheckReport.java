package com.java.sqlconverter.model;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/20, MarkHuang,new
 * </ul>
 * @since 2018/12/20
 */
public class SyntaxCheckReport {
    private boolean isSyntaxCorrect;
    private String errorMessage;
    private String correctMessage;

    public SyntaxCheckReport(boolean isSyntaxCorrect, String correctMessage, String errorMessage) {
        this.isSyntaxCorrect = isSyntaxCorrect;
        this.correctMessage = correctMessage;
        this.errorMessage = errorMessage;
    }

    public String getCorrectMessage() {
        return correctMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isSyntaxCorrect() {
        return isSyntaxCorrect;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void setSyntaxCorrect(boolean syntaxCorrect) {
        isSyntaxCorrect = syntaxCorrect;
    }
}

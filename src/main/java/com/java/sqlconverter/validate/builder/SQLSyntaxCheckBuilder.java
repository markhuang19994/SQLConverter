package com.java.sqlconverter.validate.builder;

import com.java.sqlconverter.validate.SQLSyntaxCheck;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class SQLSyntaxCheckBuilder {
    private String host;
    private String port;
    private String userName;
    private String password;
    private String database;
    private String sqlFileText;

    private SQLSyntaxCheckBuilder(){}

    public static SQLSyntaxCheckBuilder build(){
        return new SQLSyntaxCheckBuilder();
    }

    public SQLSyntaxCheckBuilder setHost(String host){
        this.host = host;
        return this;
    }

    public SQLSyntaxCheckBuilder setPort(String port) {
        this.port = port;
        return this;
    }

    public SQLSyntaxCheckBuilder setUserName(String userName) {
        this.userName = userName;
        return this;
    }

    public SQLSyntaxCheckBuilder setPassword(String password) {
        this.password = password;
        return this;
    }

    public SQLSyntaxCheckBuilder setDatabase(String password) {
        this.database = database;
        return this;
    }


    public SQLSyntaxCheckBuilder setSqlFileText(String sqlFileText) {
        this.sqlFileText = sqlFileText;
        return this;
    }

    public SQLSyntaxCheck create(){
        return new SQLSyntaxCheck(this.host, this.port, this.userName, this.password, this.sqlFileText);
    }
}

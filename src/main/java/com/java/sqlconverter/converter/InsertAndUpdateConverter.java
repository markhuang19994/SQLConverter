package com.java.sqlconverter.converter;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public interface InsertAndUpdateConverter extends com.java.sqlconverter.converter.SQLConverter {
    String convert2Upsert();
}

package com.java.sqlconverter.factory;

import com.java.sqlconverter.constant.ConvertType;
import com.java.sqlconverter.converter.InsertAndUpdateConverter;
import com.java.sqlconverter.converter.impl.InsertConverterImpl;
import com.java.sqlconverter.model.SQLDetails;

/**
 * @author MarkHuang
 * @version <ul>
 * <li>2018/12/18, MarkHuang,new
 * </ul>
 * @since 2018/12/18
 */
public class InsertAndUpdateConverterFactory {
    public static InsertAndUpdateConverter createConverter(SQLDetails sqlDetails, ConvertType convertType) {
        switch (convertType) {
            case INSERT:
                return new InsertConverterImpl(sqlDetails);
            case UPDATE:
                break;
            default:
                break;
        }
        return new InsertConverterImpl(sqlDetails);
    }
}

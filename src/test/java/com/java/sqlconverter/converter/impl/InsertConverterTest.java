package com.java.sqlconverter.converter.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.sqlconverter.SQLDetail;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mark Huang
 * @since 4/29/21
 */
class InsertConverterTest {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<Arguments> testSqlProvider() throws Exception {
        final URL resource = InsertConverterTest.class.getClassLoader().getResource("sql_data.js");
        
        assert resource != null;
        final File sqlDir = new File(resource.getPath()).getParentFile();
        final Process exec = Runtime.getRuntime().exec("node sql_data.js", null, sqlDir);
        exec.waitFor();
        final Map map = new ObjectMapper().readValue(new String(readAndCloseInputStream(exec.getInputStream())), Map.class);
        
        final Object pk = map.get("pk");
        final List data = (List) map.get("data");
        
        final List<Arguments> arguments = new ArrayList<>();
        for (Object datum : data) {
            final Map datumMap = (Map) datum;
            arguments.add(
                    Arguments.of(
                            datumMap.getOrDefault("p", ""),
                            datumMap.getOrDefault("q", ""),
                            datumMap.getOrDefault("a", ""),
                            datumMap.getOrDefault("d", "")
                    )
            );
        }
        
        return arguments.stream();
    }
    
    @ParameterizedTest(name = "#{index}-{3}")
    @MethodSource("testSqlProvider")
    void convertUpsert(String pk, String question, String answer, String desc) {
        final String sql = "--@pk:" + pk + "\n" +
                "--@upsert:on\n" + question + "\n--@upsert:off";
        final InsertConverter insertConverter = new InsertConverter(new SQLDetail(sql));
        final String result = insertConverter.convert2Upsert();
        Assertions.assertEquals(answer, trimCommentAndEmptyLine(result));
        System.out.println(result);
    }
    
    private String trimCommentAndEmptyLine(String sql) {
        return Arrays.stream(sql.split("\r?\n"))
                     .filter(l -> !l.startsWith("--") && !l.trim().isEmpty())
                     .collect(Collectors.joining("\n"));
    }
    
    private static byte[] readAndCloseInputStream(InputStream ips) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(ips);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[2048];
            for (int length; (length = bis.read(buffer)) != -1; ) {
                bos.write(buffer, 0, length);
            }
            return bos.toByteArray();
        }
    }
    
}

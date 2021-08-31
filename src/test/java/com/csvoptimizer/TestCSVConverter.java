package com.csvoptimizer;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class TestCSVConverter {

    public final static String DELIMITER = ".";

    public static void main(String[] args) throws Exception {
        testConversion("test_ms.csv");
        testConversion("test_sec.csv");
    }

    private static void testConversion(String inFileName) throws Exception {

        URL res = TestCSVConverter.class.getClassLoader().getResource(inFileName);
        File file = Paths.get(res.toURI()).toFile();
        String inPath = file.getAbsolutePath();

        String fileName = file.getName();
        int extensionIdx = fileName.lastIndexOf(DELIMITER);
        String fileNameNoExtension = extensionIdx == -1 ? fileName : fileName.substring(0, extensionIdx);
        String fileNameExtension = extensionIdx == -1 ? "" : fileName.substring(extensionIdx);
        String outPath = file.getParent() + File.separator + fileNameNoExtension + "_out" + fileNameExtension;

        Run.main(new String[]{inPath, outPath, "1", "2021-01-01", "12:00:00"});
    }
}

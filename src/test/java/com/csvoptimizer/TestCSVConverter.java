package com.csvoptimizer;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

public class TestCSVConverter {

    public static void main(String[] args) throws Exception {

        String inFileName = "test_sec.csv";

        URL res = TestCSVConverter.class.getClassLoader().getResource(inFileName);
        File file = Paths.get(res.toURI()).toFile();
        String inPath = file.getAbsolutePath();
        String outPath = file.getParent() + File.separator + "test_out.csv";

        Run.main(new String[]{inPath, outPath, "1", "2021-01-01 12:00:00"});
    }
}

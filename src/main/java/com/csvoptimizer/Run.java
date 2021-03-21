package com.csvoptimizer;

import java.io.File;
import java.nio.file.Paths;

public class Run {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.printf("*** Not enough params! ***");
            return;
        }

        // File to read data from.
        String pathToInputFile = getPathToFile(args[0]);

        // File to write data to.
        String pathToOutputFile = getPathToFile(args[1]);

        // Interval of lines to read.
        int step = 1;
        if (args.length > 2) {
            step = Integer.parseInt(args[2]);
        }

        CSVConverter converter = new CSVConverter(pathToInputFile, pathToOutputFile, step);
        converter.run();
    }

    private static String getPathToFile(String path) {

        String pathToFile = path;
        if (!Paths.get(path).isAbsolute()) {
            String currentDir = System.getProperty("user.dir");
            pathToFile = currentDir + File.separator + pathToFile;
        }

        return pathToFile;
    }
}

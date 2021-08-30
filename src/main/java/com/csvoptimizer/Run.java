package com.csvoptimizer;

import java.io.File;
import java.nio.file.Paths;

public class Run {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("*** Not enough params! ***");
            String fileName = new File(Run.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .getPath())
                    .getName();
            System.out.printf("Usage: java -jar " + fileName + " inputFile outputFile [interval=1]");
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

        String startingDate = null;
        if (args.length > 3) {
            startingDate = args[3];
        }

        CSVConverter converter = new CSVConverter(pathToInputFile, pathToOutputFile, step, startingDate);
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

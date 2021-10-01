package com.csvoptimizer;

import com.csvoptimizer.exceptions.CliParametersException;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.csvoptimizer.Constants.*;

public class Run {

    private static Map<String, Object> parseParameters(String[] args) {

        if (args.length < 1) {
            throw new CliParametersException("No mode specified");
        }

        String operation = args[0];
        if (OPERATION_AVERAGE.equalsIgnoreCase(operation)) {
            return parseParametersAver(args);
        } else if (OPERATION_ENRICH.equalsIgnoreCase(operation)) {
            return parseParametersEnr(args);
        } else {
            throw new CliParametersException("Unknown operation");
        }
    }

    /**
     * Returns parameters for AVERAGE mode.
     *
     * @return
     */
    private static Map<String, Object> parseParametersAver(String[] args) {

        if (args.length < 5) {
            throw new CliParametersException("Not enough parameters for AVERAGE mode");
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OPERATION, OPERATION_AVERAGE);
        parameters.put(PATH_TO_INPUT_FILE, args[1]);
        parameters.put(PATH_TO_OUTPUT_FILE, args[2]);
        parameters.put(COLUMN_NAME, args[3]);
        parameters.put(DEPTH, args[4]);

        return parameters;
    }

    /**
     * Returns parameters for ENRICH mode.
     *
     * @return
     */
    private static Map<String, Object> parseParametersEnr(String[] args) {

        if (args.length < 3) {
            throw new CliParametersException("Not enough parameters for ENRICH mode");
        }

        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OPERATION, OPERATION_ENRICH);

        // File to read data from.
        parameters.put(PATH_TO_INPUT_FILE, getPathToFile(args[1]));

        // File to write data to.
        parameters.put(PATH_TO_OUTPUT_FILE, getPathToFile(args[2]));

        // Interval of lines to read.
        int step = 1;
        if (args.length > 2) {
            step = Integer.parseInt(args[2]);
        }
        parameters.put(STEP, step);

        String startingDate = null;
        if (args.length > 3) {
            startingDate = args[3];

            String startingTime = "00:00:00";
            if (args.length > 4) {
                startingTime = args[4];
            }

            startingDate += " " + startingTime;
        }
        parameters.put(STARTING_DATE, startingDate);

        return parameters;
    }

    public static void main(String[] args) throws Exception {

        Map<String, Object> parameters = parseParameters(args);

        String operation = (String) parameters.get(OPERATION);

        Runnable runnable = null;

        if (OPERATION_AVERAGE.equalsIgnoreCase(operation)) {

            String pathToInputFile = (String) parameters.put(PATH_TO_INPUT_FILE, args[1]);
            String pathToOutputFile = (String) parameters.put(PATH_TO_OUTPUT_FILE, args[2]);
            String columnName = (String) parameters.put(COLUMN_NAME, args[3]);
            Integer depth = Integer.parseInt((String) parameters.put(DEPTH, args[4]));

            runnable = new Averager(pathToInputFile, pathToOutputFile, columnName, depth);

        } else if (OPERATION_ENRICH.equalsIgnoreCase(operation)) {

            String pathToInputFile = (String) parameters.get(PATH_TO_INPUT_FILE);
            String pathToOutputFile = (String) parameters.get(PATH_TO_OUTPUT_FILE);
            Integer step = (Integer) parameters.get(STEP);
            String startingDate = (String) parameters.get(STARTING_DATE);

            runnable = new CSVConverter(pathToInputFile, pathToOutputFile, step, startingDate);

        } else {
            throw new CliParametersException("Unknown mode");
        }

        runnable.run();
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

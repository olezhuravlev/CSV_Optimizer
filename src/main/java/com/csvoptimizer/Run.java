package com.csvoptimizer;

import com.csvoptimizer.exceptions.CliParametersException;

import java.io.*;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.*;

import static com.csvoptimizer.Constants.*;

public class Run {

    /**
     * Parses string having values into list.
     *
     * @param paramRow string with values.
     */
    private static LinkedHashSet<String> parseArrayValues(String paramRow) {

        // Index parameter must start with index begin attribute and end with index end attribute.
        int beginPosition = paramRow.indexOf(CLI_ARRAY_BEGIN);
        int endPosition = paramRow.indexOf(CLI_ARRAY_END);
        if (beginPosition == 0 && endPosition == paramRow.length() - 1) {
            String values = paramRow.substring(beginPosition + 1, endPosition);
            return new LinkedHashSet<>(Arrays.asList(values.split(CLI_ARRAY_DELIMITER)));
        }

        return new LinkedHashSet<>();
    }

    /**
     * Parses row that contains pair key=value.
     *
     * @param paramRow  string to parse;
     * @param delimiter delimiter between key and value;
     * @return map with the only item of key and value.
     */
    private static Map<String, Object> parseParameter(String paramRow, String delimiter) {

        Map<String, Object> result = new HashMap<>();

        String parameterUp = paramRow.toUpperCase(Locale.ROOT);
        String delimiterUp = delimiter.toUpperCase(Locale.ROOT);

        int delimiterPosition = parameterUp.indexOf(delimiterUp);
        if (delimiterPosition < 1) {
            return result;
        }

        String parameterName = paramRow.substring(0, delimiterPosition);
        String parameterValue = paramRow.substring(delimiterPosition + delimiter.length());

        // Parameter must be not empty.
        if (parameterName.isEmpty() || parameterValue.isEmpty()) {
            return result;
        }

        // If value starts with array begin attribute then this is array of attributes and should be parsed.
        if (parameterValue.indexOf(CLI_ARRAY_BEGIN) == 0) {
            LinkedHashSet<String> values = parseArrayValues(parameterValue);
            result.put(parameterName, values);
        } else {
            result.put(parameterName, parameterValue);
        }

        return result;
    }

    /**
     * Parses specified parameter from array that contains pairs key=value separated with whitespace.
     *
     * @param args      array of strings to look for specified parameter;
     * @param cliParam  string name of parameter to parse value of;
     * @param delimiter separator between key and value;
     * @return
     */
    private static String parseParameter(String[] args, String cliParam, String delimiter) {

        String result = "";

        for (int i = 0; i < args.length; i++) {

            String parameterUp = args[i].toUpperCase(Locale.ROOT);
            String cliParamUp = cliParam.toUpperCase(Locale.ROOT);
            String delimiterUp = delimiter.toUpperCase(Locale.ROOT);

            if (parameterUp.indexOf(cliParamUp + delimiterUp) != 0) {
                continue;
            }

            int delimiterPosition = parameterUp.indexOf(delimiterUp);
            result = args[i].substring(delimiterPosition + delimiter.length());

            break;
        }

        return result;
    }

    private static LinkedHashSet<String> parseParameterArray(String[] args, String cliParam) {

        for (int i = 0; i < args.length; i++) {

            String parameterUp = args[i].toUpperCase(Locale.ROOT);
            String cliParamUp = cliParam.toUpperCase(Locale.ROOT);
            String delimiterUp = CLI_ARRAY_DELIMITER.toUpperCase(Locale.ROOT);

            if (parameterUp.indexOf(cliParamUp + delimiterUp) != 0) {
                continue;
            }

            int delimiterPosition = parameterUp.indexOf(delimiterUp);
            String result = args[i].substring(delimiterPosition + CLI_ARRAY_DELIMITER.length());

            return parseArrayValues(result);
        }

        return new LinkedHashSet<>();
    }

    /**
     * Parses parameters from specified file.
     *
     * @param fileName  text file each row of it contains key and value separated with specified delimiter;
     * @param delimiter delimiter between key and value;
     * @return
     * @throws IOException map with parsed parameters and their values.
     */
    private static Map<String, Object> parseParametersFromFile(String fileName, String delimiter) throws IOException {

        Map<String, Object> parameters = new HashMap<>();

        InputStream inputStream = new FileInputStream(fileName);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String row;
        while ((row = bufferedReader.readLine()) != null) {
            Map<String, Object> paramKeyValue = parseParameter(row, delimiter);
            parameters.putAll(paramKeyValue);
        }

        return parameters;
    }

    private static Map<String, Object> parseParameters(String[] args) throws IOException {

        // File to read data from.
        String cliIn = parseParameter(args, CLI_PARAM_IN, CLI_PARAM_NAME_DELIMITER);
        String cliOut = parseParameter(args, CLI_PARAM_OUT, CLI_PARAM_NAME_DELIMITER);
        String cliStep = parseParameter(args, CLI_PARAM_STEP, CLI_PARAM_NAME_DELIMITER);
        String cliDate = parseParameter(args, CLI_PARAM_DATE, CLI_PARAM_NAME_DELIMITER);
        String cliAverDepth = parseParameter(args, CLI_PARAM_AVER_DEPTH, CLI_PARAM_NAME_DELIMITER);

        // Parameter "Columns to average values" has array of values.
        LinkedHashSet<String> cliAverColumns = parseParameterArray(args, CLI_PARAM_AVER_COLUMNS);

        Map<String, Object> parameters = new HashMap<>();

        // If some parameter provided via file with parameters then the parameters will be set from this file
        // and later can be replaced with the same parameters provided directly with CLI.
        String cliFileParameters = parseParameter(args, CLI_PARAM_FILE, CLI_PARAM_NAME_DELIMITER);
        if (!cliFileParameters.isEmpty()) {
            Map<String, Object> parametersFromFile = parseParametersFromFile(cliFileParameters, CLI_PARAM_NAME_DELIMITER);
            parameters.putAll(parametersFromFile);
        }

        // NOTE: Parameters from CLI have more precedence over parameters read from file and replace them!

        // File to read data from.
        if (!cliIn.isEmpty()) {
            parameters.put(CLI_PARAM_IN, getPathToFile(cliIn));
        }

        // File to write data to.
        if (!cliOut.isEmpty()) {
            parameters.put(CLI_PARAM_OUT, getPathToFile(cliOut));
        }

        // Interval of lines to read.
        // Parameter always presented because of having default value.
        if (cliStep.isEmpty() && !parameters.containsKey(CLI_PARAM_STEP)) {
            parameters.put(CLI_PARAM_STEP, DEFAULT_STEP);
        } else if (!cliStep.isEmpty()) {
            int step = Integer.parseInt(cliStep);
            parameters.put(CLI_PARAM_STEP, step);
        }

        // Parameter always presented because of default value.
        if (cliDate.isEmpty() && !parameters.containsKey(CLI_PARAM_DATE)) {
            parameters.put(CLI_PARAM_DATE, DEFAULT_START_DATE);
        } else if (!cliDate.isEmpty()) {
            parameters.put(CLI_PARAM_DATE, cliDate);
        }

        if (!cliAverColumns.isEmpty()) {
            parameters.put(AVER_COLUMNS, cliAverColumns);
        }

        if (!cliAverDepth.isEmpty()) {
            int averageDepth = Integer.parseInt(cliAverDepth);
            parameters.put(AVER_DEPTH, averageDepth);
        }

        return parameters;
    }

    public static void main(String[] args) throws Exception {

        Map<String, Object> parameters = parseParameters(args);

        String pathToInputFile = (String) parameters.get(CLI_PARAM_IN);
        String pathToOutputFile = (String) parameters.get(CLI_PARAM_OUT);

        // In and out files are mandatory parameters!
        if (parameters.get(CLI_PARAM_IN) == null || parameters.get(CLI_PARAM_OUT) == null) {
            throw new CliParametersException("Not i/o files specified");
        }

        LinkedHashSet<String> averColumns = (LinkedHashSet<String>) parameters.get(AVER_COLUMNS);
        Integer depth = Integer.parseInt((String) parameters.getOrDefault(AVER_DEPTH, 0));

        // If parameters for averaging set then the Averager instance should be provided.
        Averager averager = null;
        if (!averColumns.isEmpty() && depth > 0) {
            averager = new Averager(pathToInputFile, pathToOutputFile, averColumns, depth);
        }

        Integer step = Integer.parseInt((String) parameters.getOrDefault(CLI_PARAM_STEP, DEFAULT_STEP));
        String startDate = (String) parameters.getOrDefault(CLI_PARAM_DATE, DEFAULT_START_DATE);
        Calendar startingDate = retrieveDateTime(startDate);

        Runnable csvConverter = new CSVConverter(pathToInputFile, pathToOutputFile, step, startingDate, averager);
        csvConverter.run();
    }

    /**
     * Convert string representation of date and time into Calendar instance.
     *
     * @param dateTime
     * @return
     */
    private static Calendar retrieveDateTime(String dateTime) {

        // If date and time not provided the request them from user.
        if (dateTime == null || dateTime.isEmpty()) {
            System.out.println("Enter a start date for the log as '" + DATE_FORMAT_INPUT + "' (" + DEFAULT_START_DATE + "):");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                dateTime = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (dateTime == null || dateTime.isEmpty()) {
            dateTime = DEFAULT_START_DATE;
        }

        Date date = null;
        try {
            date = DATE_FORMATTER_INPUT.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        return calendar;
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

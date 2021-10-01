package com.csvoptimizer;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import static com.csvoptimizer.Constants.*;

public class CSVConverter extends AbstractRunnable {

    private String pathToInputFile;
    private String pathToOutputFile;
    private String startingDate;
    int step;

    private Map<String, FieldGenerator> generators = new HashMap<>();

    // Unmodifiable collection can be used in calculation values for another row.
    private List<String> prevResultValues;

    public CSVConverter(String pathToInputFile, String pathToOutputFile, int step, String startingDate) {

        this.pathToInputFile = pathToInputFile;
        this.pathToOutputFile = pathToOutputFile;

        if (step < 1) {
            step = 1;
        }

        this.step = step;
        this.startingDate = startingDate;

        // Initial state identified as null.
        this.prevResultValues = null;
    }

    @Override
    public void run() {

        if (startingDate == null || startingDate.isEmpty()) {
            System.out.println("Enter a starting date for the log as '" + DATE_FORMAT_INPUT + "' (" + DEFAULT_START_DATE + "):");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            try {
                startingDate = reader.readLine();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (startingDate == null || startingDate.isEmpty()) {
            startingDate = DEFAULT_START_DATE;
        }

        Date dateInput = null;
        try {
            dateInput = DATE_FORMATTER_INPUT.parse(startingDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dateInput);

        initGenerators(startDate);

        try {
            processRows(pathToInputFile, pathToOutputFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRows(String inputPath, String outputPath) throws Exception {

        float totalLines = countLines(inputPath);
        System.out.println("Lines to process: " + Math.round(totalLines));

        FileWriter outputFileWriter = new FileWriter(outputPath);
        PrintWriter printWriter = new PrintWriter(outputFileWriter);

        InputStream inputStream = new FileInputStream(inputPath);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

        String lastPrintedCounter = "";
        int currentLineCounter = -1;
        int currentStepCounter = 1;

        String[] columns = getColumnsDebugOn();

        String row;
        while ((row = bufferedReader.readLine()) != null) {

            ++currentLineCounter;

            lastPrintedCounter = printLineCounter(currentLineCounter, totalLines, lastPrintedCounter);

            // The very first line needed despite of the step because it's a header.
            if (currentLineCounter == 0) {
                printHeaderRow(columns, printWriter);
                continue;
            }

            // Specified quantity of rows should be skipped.
            if (currentStepCounter < step) {
                ++currentStepCounter;
                continue;
            }

            printRow(row, columns, printWriter);

            // Reset to initial value to start skipping further rows.
            currentStepCounter = 1;
        }
    }

    private void initGenerators(Calendar startDate) {

        // Generates value from milliseconds into format used in GPX.
        generators.put(GPX_DATE_COLUMN_HEADER, (columns, rowValues, columnIdx) -> {
            int timeColumnIdx = getColumnIndex(TIME_COLUMN_NAME);
            String currentValue = rowValues.get(timeColumnIdx);
            String timeMs = getMillisecDateFromTime(currentValue, startDate);
            return timeMs;
        });

        // Adds column value for data to be shown in user-friendly format.
        generators.put(USER_DATE_COLUMN_HEADER, (columns, rowValues, columnIdx) -> {
            int timeColumnIdx = getColumnIndex(GPX_DATE_COLUMN_HEADER);
            String timeMs = rowValues.get(timeColumnIdx).trim();
            Date date = DATE_FORMATTER_GPX.parse(timeMs);
            String userDate = DATE_FORMATTER_USER.format(date.getTime());
            return userDate;
        });

        // Adds column value for vertical speed calculated from barometer altitude.
        generators.put(V_SPEED_BARO_HEADER, (columns, rowValues, columnIdx) -> {

            if (prevResultValues == null) {
                return "0";
            }

            int timeColumnIdx = getColumnIndex(TIME_COLUMN_NAME);
            String currTimeMs = rowValues.get(timeColumnIdx).trim();
            String prevTimeMs = prevResultValues.get(timeColumnIdx).trim();
            long currTime = Long.parseLong(currTimeMs);
            long prevTime = Long.parseLong(prevTimeMs);

            int baroAltColumnIdx = getColumnIndex(BARO_ALT_COLUMN_NAME);
            String currBaroAlt = rowValues.get(baroAltColumnIdx).trim();
            String prevBaroAlt = prevResultValues.get(baroAltColumnIdx).trim();
            long currentBaroAltCm = Long.parseLong(currBaroAlt);
            long prevBaroAltCm = Long.parseLong(prevBaroAlt);

            String vSpeedCm = calculateVertSpeed(currTime, prevTime, currentBaroAltCm, prevBaroAltCm);
            return vSpeedCm;
        });

        // Sets digital representation of flight mode.
        generators.put(FLIGHT_MODE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

            int sourceColumnIdx = getColumnIndex(FLIGHT_MODE_HEADER);
            String sourceValue = rowValues.get(sourceColumnIdx);
            sourceValue = sourceValue == null ? "" : sourceValue.trim();

            String result;
            if (sourceValue.contains("ANGLE_MODE")) {
                if (sourceValue.contains("PASSTHRU") && sourceValue.contains("AUTOTUNE")) {
                    result = "3";
                } else if (sourceValue.contains("AUTOTUNE")) {
                    result = "2";
                } else {
                    result = "1";
                }
            } else {
                result = "0";
            }

            return result;
        });

        // Sets digital representation of flight state.
        generators.put(STATE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

            int sourceColumnIdx = getColumnIndex(STATE_HEADER);
            String sourceValue = rowValues.get(sourceColumnIdx);
            sourceValue = sourceValue == null ? "" : sourceValue.trim();

            String result;
            if (sourceValue.contains("GPS_FIX_HOME")) {
                if (sourceValue.contains("GPS_FIX")) {
                    result = "2";
                } else {
                    result = "1";
                }
            } else {
                result = "0";
            }

            return result;
        });

        // Sets digital representation of failsafe phase.
        generators.put(FAILSAFE_PHASE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

            int sourceColumnIdx = getColumnIndex(FAILSAFE_PHASE_HEADER);
            String sourceValue = rowValues.get(sourceColumnIdx);
            sourceValue = sourceValue == null ? "" : sourceValue.trim();

            String result;
            if ("6".equals(sourceValue)) {
                result = "6";
            } else {
                result = "0";
            }

            return result;
        });

        // Sets digital representation of icon to show.
        generators.put(STATUS_ICON_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

            int flightColumnIdx = getColumnIndex(FLIGHT_MODE_INDICATOR_HEADER);
            String flight = rowValues.get(flightColumnIdx);
            flight = flight == null ? "" : flight.trim();

            int failsafeColumnIdx = getColumnIndex(FAILSAFE_PHASE_INDICATOR_HEADER);
            String failsafe = rowValues.get(failsafeColumnIdx);
            failsafe = failsafe == null ? "" : failsafe.trim();

            if ("3".equals(flight) || "6".equals(failsafe)) {
                return "1";
            } else {
                return "0";
            }
        });
    }

    // Returns value of a column as original or generated (transformed) from original content.
    private String generateValue(String[] columns, List<String> rowValues, int columnIdx) throws Exception {

        if (columnIdx >= columns.length) {
            throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUND_MESSAGE);
        }

        // If current column index exceeds number of columns of values then such column should be created.
        String columnValue = "";
        if (columnIdx < rowValues.size()) {
            columnValue = rowValues.get(columnIdx);
        } else {
            columnValue = "";
            rowValues.add(columnValue);
        }

        // If there is no generator for the specified column the value returned as it is.
        String columnName = columns[columnIdx];
        if (!generators.containsKey(columnName)) {
            return columnValue;
        }

        // Obtain value using designated generator.
        FieldGenerator generator = generators.get(columnName);
        String result = generator.generateValue(columns, rowValues, columnIdx);
        return result;
    }

    // Outputs values of another row into result file.
    private void printRow(String row, String[] columns, PrintWriter printWriter) throws Exception {

        String[] rowValues = row.split(CSV_DELIMITER, -1);

        // From the beginning the result is filled with initial values.
        List<String> resultValues = new ArrayList<>(Arrays.asList(rowValues));
        for (int i = 0; i < columns.length; ++i) {
            String result = generateValue(columns, resultValues, i);
            resultValues.set(i, result);
        }

        // Store current result to use it for calculations of the next row.
        prevResultValues = Collections.unmodifiableList(resultValues);

        printRowData(resultValues, printWriter);
    }

    /**
     * Returns date in format of milliseconds (e.g. "2000-01-01T00:00:41.092541Z").
     *
     * @param timeMilliseconds
     * @param startDate
     * @return
     */
    private String getMillisecDateFromTime(String timeMilliseconds, final Calendar startDate) {

        // Last 6 digits stand for milliseconds.
        int endPosition = timeMilliseconds.length() - 6;
        if (endPosition < 0) {
            endPosition = 0;
        }

        String milliseconds = timeMilliseconds.substring(endPosition).trim();
        int millisecondsInt = parseInt(milliseconds);
        String secondsString = getComplementedValue(millisecondsInt, 6, "0");

        String seconds = timeMilliseconds.substring(0, endPosition).trim();
        int secondsInt = parseInt(seconds);

        // We need to create a copy of current calendar not to spoil it for using in the next iteration!
        Calendar rowDate = (Calendar) startDate.clone();
        rowDate.add(Calendar.SECOND, secondsInt);

        String startingDate = DATE_FORMATTER_GPX.format(rowDate.getTime());
        return startingDate + "." + secondsString + "Z";
    }

    private String calculateVertSpeed(long currTimeMs, long prevTimeMs, long currBaroAltCm, long prevBaroAltCm) {

        long timeIntervalMillis = Math.abs(currTimeMs - prevTimeMs);

        if (timeIntervalMillis == 0) {
            return "0";
        }

        long heightInterval = currBaroAltCm - prevBaroAltCm;
        double vertSpeedCmSec = heightInterval / (double) timeIntervalMillis * 1000000;

        return String.valueOf(vertSpeedCmSec);
    }

    public static int parseInt(String val) {

        if (val == null) {
            return 0;
        }

        try {
            int result = Integer.parseInt(val);
            return result;
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static String getComplementedValue(int val, int length, String symbol) {

        String valueStr = String.valueOf(val);

        int symbolsToAdd = length - valueStr.length();
        String add = "";
        for (int i = 0; i < symbolsToAdd; ++i) {
            add += symbol;
        }

        return add + valueStr;
    }

    @Override
    protected void processRow(Map<String, Object> parameters) throws Exception {

        String row = (String) parameters.get("row");
        String[] columns = (String[]) parameters.get("columns");
        PrintWriter printWriter = (PrintWriter) parameters.get("printWriter");

        printRow(row, columns, printWriter);
    }
}

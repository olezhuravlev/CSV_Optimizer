package com.csvoptimizer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSVConverter {

    public static final String DEFAULT_START_DATE = "2021-01-01 12:00:00";

    private final String CSV_DELIMITER = ",";

    private final String DATE_FORMAT_INPUT = "yyyy-MM-dd HH:mm:ss";
    private final String DATE_FORMAT_USER = DATE_FORMAT_INPUT;
    private final String DATE_FORMAT_GPX = "yyyy-MM-dd'T'HH:mm:ss";
    private final String DATE_FORMAT_MS = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    private final Locale DATE_LOCALE = Locale.ENGLISH;

    private SimpleDateFormat DATE_FORMATTER_INPUT = new SimpleDateFormat(DATE_FORMAT_INPUT, DATE_LOCALE);
    private SimpleDateFormat DATE_FORMATTER_USER = new SimpleDateFormat(DATE_FORMAT_USER, DATE_LOCALE);
    private SimpleDateFormat DATE_FORMATTER_GPX = new SimpleDateFormat(DATE_FORMAT_GPX, DATE_LOCALE);
    private SimpleDateFormat DATE_FORMATTER_MS = new SimpleDateFormat(DATE_FORMAT_MS, DATE_LOCALE);

    private final String INDEX_OUT_OF_BOUND_MESSAGE = "Wrong column index!";

    // Columns used in calculations.
    private final String TIME_COLUMN_NAME = "time (us)";
    private final String BARO_ALT_COLUMN_NAME = "BaroAlt (cm)";

    // Additional columns.
    // Column header for date compatible with GPX-format (e.g. "2000-01-01T00:00:41.092541Z").
    private final String GPX_DATE_COLUMN_HEADER = "gpxDate";

    // Column header for date to be shown in prescribed format.
    private final String USER_DATE_COLUMN_HEADER = "userDate";

    // Column header for vertical speed calculated from change of barometer altitude.
    private final String V_SPEED_BARO_HEADER = "vSpeedBaroAlt (cm/s)";

    // Digital representation of flight mode flags.
    private final String FLIGHT_MODE_HEADER = "flightModeFlags (flags)";
    private final String FLIGHT_MODE_INDICATOR_HEADER = "flightModeIndicator";

    // Digital representation of state flags.
    private final String STATE_HEADER = "stateFlags (flags)";
    private final String STATE_INDICATOR_HEADER = "stateIndicator";

    // Digital representation of failsafe phase.
    private final String FAILSAFE_PHASE_HEADER = "failsafePhase (flags)";
    private final String FAILSAFE_PHASE_INDICATOR_HEADER = "failsafePhaseIndicator";

    // Icon to display flight mode, state or failsafe phase.
    private final String STATUS_ICON_INDICATOR_HEADER = "statusIconIndicator";

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

    public void run() throws Exception {

        if (startingDate == null || startingDate.isEmpty()) {
            System.out.println("Enter a starting date for the log as '" + DATE_FORMAT_INPUT + "' (" + DEFAULT_START_DATE + "):");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            startingDate = reader.readLine();
        }

        if (startingDate == null || startingDate.isEmpty()) {
            startingDate = DEFAULT_START_DATE;
        }

        Date dateInput = DATE_FORMATTER_INPUT.parse(startingDate);
        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dateInput);

        initGenerators(startDate);

        processRows(pathToInputFile, pathToOutputFile);
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

    private int getColumnIndex(String columnName) {

        String[] columnsArray = getColumnsDebugOn();

        Integer found = null;

        for (int i = 0; i < columnsArray.length; ++i) {
            if (columnsArray[i].equals(columnName)) {
                found = i;
                break;
            }
        }

        if (found == null) {
            throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUND_MESSAGE);
        }

        return found;
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

    // Return array of columns located BEFORE debug columns in imported file.
    private String[] getColumnsBeforeDebug() {
        return new String[]{
                "loopIteration",
                "time (us)",
                "axisP[0]",
                "axisP[1]",
                "axisP[2]",
                "axisI[0]",
                "axisI[1]",
                "axisI[2]",
                "axisD[0]",
                "axisD[1]",
                "axisF[0]",
                "axisF[1]",
                "axisF[2]",
                "rcCommand[0]",
                "rcCommand[1]",
                "rcCommand[2]",
                "rcCommand[3]",
                "setpoint[0]",
                "setpoint[1]",
                "setpoint[2]",
                "setpoint[3]",
                "vbatLatest (V)",
                "amperageLatest (A)",
                "BaroAlt (cm)",
                "rssi",
                "gyroADC[0]",
                "gyroADC[1]",
                "gyroADC[2]",
                "accSmooth[0]",
                "accSmooth[1]",
                "accSmooth[2]"
        };
    }

    // Return array of columns used as blackbox debug columns in imported file.
    private String[] getColumnsDebug() {
        return new String[]{
                "debug[0]",
                "debug[1]",
                "debug[2]",
                "debug[3]"
        };
    }

    // Return array of columns located AFTER debug columns in imported file.
    private String[] getColumnsAfterDebug() {
        return new String[]{
                "motor[0]",
                "motor[1]",
                "motor[2]",
                "motor[3]",
                "energyCumulative (mAh)",
                FLIGHT_MODE_HEADER,
                STATE_HEADER,
                FAILSAFE_PHASE_HEADER,
                "rxSignalReceived",
                "rxFlightChannelsValid",
                "GPS_numSat",
                "GPS_coord[0]",
                "GPS_coord[1]",
                "GPS_altitude",
                "GPS_speed (m/s)",
                "GPS_ground_course",
                GPX_DATE_COLUMN_HEADER,
                USER_DATE_COLUMN_HEADER,
                V_SPEED_BARO_HEADER,
                FLIGHT_MODE_INDICATOR_HEADER,
                STATE_INDICATOR_HEADER,
                FAILSAFE_PHASE_INDICATOR_HEADER,
                STATUS_ICON_INDICATOR_HEADER
        };
    }

    // Returns full collection of columns used in imported file including columns of blackbox debug mode.
    private String[] getColumnsDebugOn() {

        List<String> result = new ArrayList<>(Arrays.asList(getColumnsBeforeDebug()));
        result.addAll(Arrays.asList(getColumnsDebug()));
        result.addAll(Arrays.asList(getColumnsAfterDebug()));

        return result.toArray(new String[0]);
    }

    private int countLines(String fileName) throws IOException {
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        int lines = 0;
        while (bufferedReader.readLine() != null) lines++;
        bufferedReader.close();
        return lines;
    }

    private String printLineCounter(float currentLineCounter, float totalLines, String lastPrintedCounter) {

        String lineCounterToPrint = String.valueOf(Math.round(currentLineCounter / totalLines * 100));

        if (lineCounterToPrint.equals(lastPrintedCounter)) {
            return lastPrintedCounter;
        }

        System.out.print("\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print(lineCounterToPrint + "% ");
        System.out.flush();

        return lineCounterToPrint;
    }

    private void printHeaderRow(String[] columns, PrintWriter printWriter) {
        List<String> rowData = new ArrayList<>(Arrays.asList(columns));
        printRowData(rowData, printWriter);
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

    private void printRowData(List<String> rowData, PrintWriter printWriter) {
        String[] resultValues = rowData.toArray(new String[0]);
        String stringToWrite = String.join(CSV_DELIMITER, resultValues);
        printWriter.println(stringToWrite);
        printWriter.flush();
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
}

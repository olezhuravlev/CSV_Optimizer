package com.csvoptimizer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class CSVConverter {

    private final String CSV_DELIMITER = ",";
    private final String TIME_COLUMN_NAME = "time (us)";
    private final String DATE_FORMAT_INPUT = "yyyy-MM-dd HH:mm:ss";
    private final String DEFAULT_START_DATE = "2021-01-01 12:00:00";
    private final String DATE_FORMAT_GPX = "yyyy-MM-dd'T'HH:mm:ss";

    private final Locale DATE_LOCALE = Locale.ENGLISH;

    private SimpleDateFormat DATE_FORMATTER_INPUT = new SimpleDateFormat(DATE_FORMAT_INPUT, DATE_LOCALE);

    private String DATE_FORMAT_USER = DATE_FORMAT_INPUT;
    private SimpleDateFormat DATE_FORMATTER_USER = new SimpleDateFormat(DATE_FORMAT_USER, DATE_LOCALE);
    private SimpleDateFormat DATE_FORMATTER_GPX = new SimpleDateFormat(DATE_FORMAT_GPX, DATE_LOCALE);

    private final String INDEX_OUT_OF_BOUND_MESSAGE = "Wrong column index!";

    // Additional classes.
    // Column header for data to be shown in prescribed format.
    private final String USER_DATE_COLUMN_HEADER = "userDate";

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
    int step;

    private Map<String, FieldTransformer> transformers = new HashMap<>();

    public CSVConverter(String pathToInputFile, String pathToOutputFile, int step) {

        this.pathToInputFile = pathToInputFile;
        this.pathToOutputFile = pathToOutputFile;

        if (step < 1) {
            step = 1;
        }

        this.step = step;
    }

    public void run() throws Exception {

        System.out.println("Enter a starting date for the log as '" + DATE_FORMAT_INPUT + "' (" + DEFAULT_START_DATE + "):");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String dateStringInput = reader.readLine();

        if (dateStringInput.isEmpty()) {
            dateStringInput = DEFAULT_START_DATE;
        }

        Date dateInput = DATE_FORMATTER_INPUT.parse(dateStringInput);

        Calendar startDate = Calendar.getInstance();
        startDate.setTime(dateInput);
        initTransformers(startDate);

        File inputFile = new File(pathToInputFile);

        InputStream inputStream = new FileInputStream(inputFile);
        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        FileWriter outputFileWriter = new FileWriter(pathToOutputFile);
        PrintWriter printWriter = new PrintWriter(outputFileWriter);

        float totalLines = countLines(pathToInputFile);
        System.out.println("Lines to process: " + Math.round(totalLines));

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

            if (currentStepCounter < step) {
                ++currentStepCounter;
                continue;
            }

            printRow(row, columns, printWriter);
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

    private void initTransformers(Calendar startDate) {

        // Transforms value from nanoseconds into format used in GPX.
        transformers.put(TIME_COLUMN_NAME, (columns, rowValues, columnIdx) -> {
            String currentValue = rowValues.get(columnIdx);
            String gpxDate = getGPXDateFromTime(currentValue, startDate);
            return gpxDate;
        });

        // Adds column value for data to be shown in prescribed format.
        transformers.put(USER_DATE_COLUMN_HEADER, (columns, rowValues, columnIdx) -> {
            // By that moment column with date already has time in GPX-format.
            int sourceColumnIdx = getColumnIndex(TIME_COLUMN_NAME);
            String gpxDate = rowValues.get(sourceColumnIdx).trim();
            Date date = DATE_FORMATTER_GPX.parse(gpxDate);
            String userDate = DATE_FORMATTER_USER.format(date.getTime());

            return userDate;
        });

        // Sets digital representation of flight mode.
        transformers.put(FLIGHT_MODE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

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
        transformers.put(STATE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

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
        transformers.put(FAILSAFE_PHASE_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

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
        transformers.put(STATUS_ICON_INDICATOR_HEADER, (columns, rowValues, columnIdx) -> {

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
                USER_DATE_COLUMN_HEADER,
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

    private String transformValue(String[] columns, List<String> rowValues, int columnIdx) throws Exception {

        if (columnIdx >= columns.length) {
            throw new IndexOutOfBoundsException(INDEX_OUT_OF_BOUND_MESSAGE);
        }

        String columnName = columns[columnIdx];

        // If current column index exceeds number of columns of values then such column should be created.
        String columnValue = "";
        if (columnIdx < rowValues.size()) {
            columnValue = rowValues.get(columnIdx);
        } else {
            columnValue = "";
            rowValues.add(columnValue);
        }

        // If there is no transformer for the specified column the value returned as it is.
        if (!transformers.containsKey(columnName)) {
            return columnValue;
        }

        FieldTransformer transformer = transformers.get(columnName);
        String result = transformer.transform(columns, rowValues, columnIdx);
        return result;
    }

    private void printRow(String row, String[] columns, PrintWriter printWriter) throws Exception {

        String[] rowValues = row.split(CSV_DELIMITER, -1);

        // From the beginning result filled with initial values - it's handy for further transformation
        // of already transformed values.
        List<String> resultValues = new ArrayList<>(Arrays.asList(rowValues));
        for (int i = 0; i < columns.length; ++i) {
            String result = transformValue(columns, resultValues, i);
            resultValues.set(i, result);
        }

        printRowData(resultValues, printWriter);
    }

    private void printRowData(List<String> rowData, PrintWriter printWriter) {
        String[] resultValues = rowData.toArray(new String[0]);
        String stringToWrite = String.join(CSV_DELIMITER, resultValues);
        printWriter.println(stringToWrite);
        printWriter.flush();
    }

    /**
     * Returns date in format "2000-01-01T00:00:41.092541Z".
     *
     * @param timeNanoseconds
     * @param startDate
     * @return
     */
    private String getGPXDateFromTime(String timeNanoseconds, final Calendar startDate) {

        // Last 6 digits stand for nanoseconds.
        int endPosition = timeNanoseconds.length() - 6;
        if (endPosition < 0) {
            endPosition = 0;
        }

        String nanoseconds = timeNanoseconds.substring(endPosition).trim();
        int nanosecondsInt = parseInt(nanoseconds);
        String nanosecondsString = getComplementedValue(nanosecondsInt, 6, "0");

        String seconds = timeNanoseconds.substring(0, endPosition).trim();
        int secondsInt = parseInt(seconds);

        // We need to create a copy of current calendar not to spoil it for using in the next iteration!
        Calendar rowDate = (Calendar) startDate.clone();
        rowDate.add(Calendar.SECOND, secondsInt);

        String startingDate = DATE_FORMATTER_GPX.format(rowDate.getTime());
        return startingDate + "." + nanosecondsString + "Z";
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

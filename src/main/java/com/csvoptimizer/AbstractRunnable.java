package com.csvoptimizer;

import java.io.*;
import java.util.*;

import static com.csvoptimizer.Constants.*;

public abstract class AbstractRunnable implements Runnable {

    protected int countLines(String fileName) throws IOException {
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        int lines = 0;
        while (bufferedReader.readLine() != null) lines++;
        bufferedReader.close();
        return lines;
    }

    /**
     * Returns full collection of columns used in imported file including columns of blackbox debug mode.
     *
     * @return
     */
    protected String[] getColumnsDebugOn() {

        List<String> result = new ArrayList<>(Arrays.asList(getColumnsBeforeDebug()));
        result.addAll(Arrays.asList(getColumnsDebug()));
        result.addAll(Arrays.asList(getColumnsAfterDebug()));

        return result.toArray(new String[0]);
    }

    /**
     * Returns array of columns located BEFORE debug columns in imported file.
     */
    protected String[] getColumnsBeforeDebug() {
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

    /**
     * Returns array of columns used as blackbox debug columns in imported file.
     */
    protected String[] getColumnsDebug() {
        return new String[]{
                "debug[0]",
                "debug[1]",
                "debug[2]",
                "debug[3]"
        };
    }

    /**
     * Return array of columns located AFTER debug columns in imported file.
     */
    protected String[] getColumnsAfterDebug() {
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

    protected void processRows(Map<String, Object> parameters) throws Exception {

        String inputPath = (String) parameters.get(CLI_PARAM_IN);
        String outputPath = (String) parameters.get(CLI_PARAM_OUT);
        int step = (int) parameters.get(CLI_PARAM_STEP);

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

        Map<String, Object> rowParameters = new HashMap<>();
        rowParameters.put(PRINT_WRITER, printWriter);
        rowParameters.put(COLUMNS, columns);

        String row;
        while ((row = bufferedReader.readLine()) != null) {

            ++currentLineCounter;

            lastPrintedCounter = printLineCounter(currentLineCounter, totalLines, lastPrintedCounter);

            if (currentLineCounter == 0) {
                // The very first line needed despite of the step because it's a header.
                printHeaderRow(columns, printWriter);
                continue;
            } else if (currentStepCounter < step) {
                // Specified quantity of rows should be skipped.
                ++currentStepCounter;
                continue;
            }

            rowParameters.put("row", row);
            processRow(rowParameters);

            // Reset to initial value to start skipping further rows.
            currentStepCounter = 1;
        }
    }

    protected String printLineCounter(float currentLineCounter, float totalLines, String lastPrintedCounter) {

        String lineCounterToPrint = String.valueOf(Math.round(currentLineCounter / totalLines * 100));

        if (lineCounterToPrint.equals(lastPrintedCounter)) {
            return lastPrintedCounter;
        }

        System.out.print("\b\b\b\b\b\b\b\b\b\b\b");
        System.out.print(lineCounterToPrint + "% ");
        System.out.flush();

        return lineCounterToPrint;
    }

    protected void printHeaderRow(String[] columns, PrintWriter printWriter) {
        List<String> rowData = new ArrayList<>(Arrays.asList(columns));
        printRowData(rowData, printWriter);
    }

    protected void printRowData(List<String> rowData, PrintWriter printWriter) {
        String[] resultValues = rowData.toArray(new String[0]);
        String stringToWrite = String.join(CSV_DELIMITER, resultValues);
        printWriter.println(stringToWrite);
        printWriter.flush();
    }

    protected int getColumnIndex(String columnName) {

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

    /**
     * Implementation provides certain processing of each row.
     *
     * @param parameters
     */
    protected abstract void processRow(Map<String, Object> parameters) throws Exception;
}

package com.csvoptimizer;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.csvoptimizer.Constants.*;

public class Averager extends AbstractRunnable {

    private String pathToInputFile;
    private String pathToOutputFile;
    private String columnName;
    int depth;

    private EvictingQueue queue;
    private String delimiter;
    private Class type;

    public Averager(String pathToInputFile, String pathToOutputFile, String columnName, int depth) {

        this.pathToInputFile = pathToInputFile;
        this.pathToOutputFile = pathToOutputFile;

        if (depth < 1) {
            depth = 1;
        }

        this.depth = depth;
        this.columnName = columnName;
    }

    @Override
    public void run() {
        try {

            Map<String, Object> parameters = new HashMap<>();
            parameters.put(PATH_TO_INPUT_FILE, pathToInputFile);
            parameters.put(PATH_TO_OUTPUT_FILE, pathToOutputFile);

            // For averaging step is always 1 because every account is taken into account.
            parameters.put(STEP, 1);

            parameters.put(COLUMN_NAME, columnName);
            parameters.put(DEPTH, depth);

            processRows(parameters);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void processRow(Map<String, Object> parameters) throws Exception {

        Map<String, Object> rowParameters = new HashMap<>();
        PrintWriter printWriter = (PrintWriter) parameters.get(PRINT_WRITER);
        String[] columns = (String[]) parameters.get(COLUMNS);
        String row = (String) parameters.get(ROW);

        // Take value for the specified column and define if it has decimal separator.
        String[] rowValues = row.split(CSV_DELIMITER, -1);
        //List<String> resultValues = new ArrayList<>(Arrays.asList(rowValues));
        int timeColumnIdx = getColumnIndex(columnName);
        String currentValue = rowValues[timeColumnIdx];

        // Initialize delimiter and type using type of the first value.
        if (delimiter == null) {

            delimiter = getDelimiter(currentValue);

            if (delimiter.isEmpty()) {
                type = Integer.class;
            } else {
                type = Double.class;
            }
        }

        // Initialize queue with certain data type (with or without decimal separator).
        if (queue == null) {
            queue = initQueue(delimiter);
        }

        if (type == Integer.class) {
            Integer val = Integer.parseInt(currentValue);
            queue.put(val);
            Integer average = calculateAverageInteger(queue);
        } else if (type == Double.class) {
            Double val = Double.parseDouble(currentValue);
            queue.put(val);
            Double average = calculateAverageDouble(queue);
        }
    }

    private Integer calculateAverageInteger(EvictingQueue<Integer> val) {

        int size = queue.size();
        if (size == 0) {
            return 0;
        }

        Integer summ = (Integer) queue.stream().collect(Collectors.summarizingInt(Integer::intValue));

        return summ / size;
    }

    private Double calculateAverageDouble(EvictingQueue<Double> val) {

        int size = queue.size();
        if (size == 0) {
            return 0D;
        }

        Double summ = (Double) queue.stream().collect(Collectors.summarizingDouble(Double::doubleValue));

        return summ / size;
    }

    private String getDelimiter(String value) {

        String result = "";

        if (value.indexOf(POINT_DELIMITER) > -1) {
            result = POINT_DELIMITER;
        } else if (value.indexOf(COMMA_DELIMITER) > -1) {
            result = COMMA_DELIMITER;
        }

        return result;
    }

    /**
     * Returs queue initialized with certain item type (int or decimal if there is a decimal separator).
     *
     * @param delimiter to define type of data the queue holds.
     */
    private EvictingQueue initQueue(String delimiter) {

        EvictingQueue result;

        // Just check if any delimiter exists.
        if (delimiter.isEmpty()) {
            result = new EvictingQueue<Integer>(depth);
        } else {
            result = new EvictingQueue<Double>(depth);
        }

        return result;
    }
}

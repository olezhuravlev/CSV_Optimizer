package com.csvoptimizer;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Run {

    public static void main(String[] args) {

        if (args.length < 3) {
            System.out.printf("*** Not enough params! ***");
            return;
        }

        float linesCount = 0;

        // File to read data from.
        String pathToInputFile = args[0];
        try {
            linesCount = countLines(pathToInputFile);
            System.out.println("Lines to process: " + Math.round(linesCount));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // File to write data to.
        String pathToOutputFile = args[1];

        // Interval of lines to read.
        int interval = Integer.parseInt(args[2]);
        if (interval < 1) {
            interval = 1;
        }

        File inputFile = new File(pathToInputFile);

        try {
            InputStream inputStream = new FileInputStream(inputFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            FileWriter outputFileWriter = new FileWriter(pathToOutputFile);
            PrintWriter printWriter = new PrintWriter(outputFileWriter);

            float lineCounter = 0;
            String lineCounterLastPrinted = "";
            float intervalCounter = 1;
            String line;
            while ((line = bufferedReader.readLine()) != null) {

                ++lineCounter;

                String lineCounterToPrint = String.valueOf(Math.round(lineCounter / linesCount * 100));
                if (!lineCounterLastPrinted.equals(lineCounterToPrint)) {
                    System.out.print(lineCounterToPrint + "% ");
                    lineCounterLastPrinted = lineCounterToPrint;
                }

                // First line always needed because it's a header.
                if (lineCounter > 1 && intervalCounter < interval) {
                    ++intervalCounter;
                    continue;
                }

                printWriter.println(line);
                printWriter.flush();
                intervalCounter = 1;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countLines(String fileName) throws IOException {
        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        int lines = 0;
        while (bufferedReader.readLine() != null) lines++;
        bufferedReader.close();
        return lines;
    }
}

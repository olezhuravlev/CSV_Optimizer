package com.csvoptimizer.exceptions;

import com.csvoptimizer.Run;

import java.io.File;

import static com.csvoptimizer.Constants.DEFAULT_START_DATE;

public class CliParametersException extends RuntimeException {

    public CliParametersException(String message) {
        super(message);
    }

    @Override
    public String getMessage() {

        String result = "***" + super.getMessage() + "***" + System.lineSeparator();

        String fileName = new File(Run.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();

        result += fileName + System.lineSeparator();

        String usage = "Usage for averaging: java -jar " + fileName + " AVER inputFile outputFile [interval=1] [startDate=" + DEFAULT_START_DATE + "]";
        result += usage + System.lineSeparator();

        usage = "Usage for adding new fields: java -jar " + fileName + " ENR inputFile outputFile columnName [depth=10]";
        result += usage + System.lineSeparator();

        return result;
    }
}

package com.csvoptimizer;

import java.util.List;

public interface FieldGenerator {
    String generateValue(String[] columns, List<String> rowValues, int columnIdx) throws Exception;
}

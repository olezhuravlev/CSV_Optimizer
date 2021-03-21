package com.csvoptimizer;

import java.util.List;

public interface FieldTransformer {
    String transform(String[] columns, List<String> rowValues, int columnIdx) throws Exception;
}

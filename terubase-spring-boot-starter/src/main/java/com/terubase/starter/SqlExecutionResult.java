package com.terubase.starter;

import java.util.List;
import java.util.Map;

public record SqlExecutionResult(
        boolean query,
        int updateCount,
        int[] batchUpdateCounts,
        List<Map<String, Object>> rows,
        String message
) {
    public static SqlExecutionResult query(List<Map<String, Object>> rows) {
        return new SqlExecutionResult(true, -1, new int[0], rows, "Query executed successfully");
    }

    public static SqlExecutionResult update(int updateCount) {
        return new SqlExecutionResult(false, updateCount, new int[0], List.of(), "Mutation executed successfully");
    }

    public static SqlExecutionResult batch(int[] batchUpdateCounts) {
        return new SqlExecutionResult(false, -1, batchUpdateCounts, List.of(), "Batch executed successfully");
    }
}

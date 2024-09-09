package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering;

import io.micrometer.observation.annotation.Observed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterUtils {

    // Constants for Pageable-related parameters
    private static final Set<String> EXCLUDED_PARAMS = Set.of("page", "size", "sort");

    /**
     * Parses filter parameters from the query string and builds a list of FilterCriteria.
     * Excludes 'page', 'size', and 'sort' parameters that belong to Pageable.
     *
     * @param filterParams Map of query parameters representing filters
     * @return List of FilterCriteria built from query parameters
     */
    @Observed
    public static List<FilterCriteria> buildFilterCriteria(Map<String, String> filterParams) {
        List<FilterCriteria> filters = new ArrayList<>();

        filterParams.forEach((key, value) -> {
            if (isFilterable(key)) {
                filters.add(buildCriteria(key, value));
            }
        });

        return filters;
    }

    // Helper method to check if a key is filterable (i.e., not excluded like 'page', 'size', 'sort')
    private static boolean isFilterable(String key) {
        return !EXCLUDED_PARAMS.contains(key.toLowerCase());
    }

    // Helper method to build FilterCriteria based on the value
    private static FilterCriteria buildCriteria(String key, String value) {
            if (key != null && key.contains(">")) {
                String[] parts = key.split(">");
                return new FilterCriteria(parts[0], ">", parts[1]);
            } else if (key != null && key.contains("<")) {
                String[] parts = key.split("<");
                return new FilterCriteria(parts[0], "<", parts[1]);
            } else {
                return new FilterCriteria(key, ":", value); // Default is equality or LIKE
            }
        }

}

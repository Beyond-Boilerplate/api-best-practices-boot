package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.caching;

import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.FilterCriteria;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.stream.Collectors;

public class PageFilterSortCacheKeyGenerator {
    /**
     * Generates a unique key based on the filter criteria, pagination, and sorting parameters.
     *
     * @param filters  List of FilterCriteria used for filtering
     * @param pageable Pageable object that contains pagination and sorting information
     * @return A unique string representation of the filters, pagination, and sorting
     */
    public static String generateKey(List<FilterCriteria> filters, Pageable pageable) {
        StringBuilder keyBuilder = new StringBuilder();

        // Append pageable information (page, size, sort)
        keyBuilder.append("page=").append(pageable.getPageNumber()).append("_");
        keyBuilder.append("size=").append(pageable.getPageSize()).append("_");

        Sort sort = pageable.getSort();
        if (sort.isSorted()) {
            String sortStr = sort.stream()
                    .map(order -> order.getProperty() + "," + order.getDirection())
                    .collect(Collectors.joining("_"));
            keyBuilder.append("sort=").append(sortStr).append("_");
        }

        // Append filter criteria
        if (filters != null && !filters.isEmpty()) {
            String filtersStr = filters.stream()
                    .map(filter -> filter.getKey() + filter.getOperation() + filter.getValue())
                    .collect(Collectors.joining("_"));
            keyBuilder.append("filters=").append(filtersStr);
        } else {
            keyBuilder.append("filters=no-filters");
        }

        return keyBuilder.toString();
    }
}

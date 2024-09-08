package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FilterCriteria {
    private String key;        // The field name
    private String operation;  // The operation (e.g., ":", ">", "<", "=")
    private Object value;      // The value to filter by
}

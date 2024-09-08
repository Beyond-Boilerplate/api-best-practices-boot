package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.model;
import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaginatedTransaction {
    private List<Transaction> transactions;
    private long total;
}

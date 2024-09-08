package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.controllers;

import com.github.sardul3.io.api_best_practices_boot.eTags.config.ETagGenerator;
import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.eTags.services.TransactionService;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.FilterCriteria;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.FilterUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v2/transactions")
@Slf4j
public class TransactionsControllerPaged {

    private final TransactionService transactionService;

    public TransactionsControllerPaged(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<Page<Transaction>> getTransactions(
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestParam Map<String, String> filterParams,
            Pageable pageable) {
        log.info("getTransactions called with filters: {}, pageInfo {}", filterParams, pageable);
        // Build the list of filters to be applied
        List<FilterCriteria> filters = FilterUtils.buildFilterCriteria(filterParams);

         Page<Transaction> transactions = transactionService.getAllTransactionsWithPage(filters, pageable);

        String eTag = ETagGenerator.generateETag(transactions.stream().toList());

        if (ifNoneMatch != null && ifNoneMatch.equals(eTag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(eTag).build();
        }

        return ResponseEntity.ok().eTag(eTag).body(transactions);
    }
}

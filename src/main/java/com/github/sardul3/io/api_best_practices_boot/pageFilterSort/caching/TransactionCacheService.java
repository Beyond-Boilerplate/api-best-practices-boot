package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.caching;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.eTags.repos.TransactionRepository;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.FilterCriteria;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering.TransactionSpecificationBuilder;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.model.PaginatedTransaction;
import io.micrometer.observation.annotation.Observed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionCacheService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Observed(name = "transactions.all",
            contextualName = "db-or-cache-get-all-transactions",
            lowCardinalityKeyValues = {"GET", "transactions"})
    @Cacheable(
            value = "transactionsPFSCache",
            key = "T(com.github.sardul3.io.api_best_practices_boot.pageFilterSort.caching.PageFilterSortCacheKeyGenerator).generateKey(#filters, #pageable)",
            unless = "#result == null"
    )
    public PaginatedTransaction getAllTransactionsWithCache(List<FilterCriteria> filters, Pageable pageable) {
        TransactionSpecificationBuilder builder = new TransactionSpecificationBuilder();
        filters.forEach(filter -> builder.with(filter.getKey(), filter.getOperation(), filter.getValue()));
        Specification<Transaction> spec = builder.build();

        // Use pageable in findAll to ensure sorting and pagination are applied
        Page<Transaction> pageResult = transactionRepository.findAll(spec, pageable);

        // Extract and return the content as a List
        return
                new PaginatedTransaction( pageResult.getContent(), pageResult.getTotalElements());
    }
}

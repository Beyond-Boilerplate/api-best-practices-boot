package com.github.sardul3.io.api_best_practices_boot.pageFilterSort.filtering;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class TransactionSpecificationBuilder {
    private final List<FilterCriteria> params;

    public TransactionSpecificationBuilder() {
        this.params = new ArrayList<FilterCriteria>();
    }

    public TransactionSpecificationBuilder with(String key, String operation, Object value) {
        params.add(new FilterCriteria(key, operation, value));
        return this;
    }

    public Specification<Transaction> build() {
        if (params.isEmpty()) {
            return null;
        }
        Specification<Transaction> result = new TransactionSpecification(params.getFirst());
        for (int i = 1; i < params.size(); i++) {
            result = Specification.where(result).and(new TransactionSpecification(params.get(i)));
        }
        return result;
    }
}

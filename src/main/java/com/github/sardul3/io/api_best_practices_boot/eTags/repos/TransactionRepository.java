package com.github.sardul3.io.api_best_practices_boot.eTags.repos;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccount(String fromAccount);
    List<Transaction> findByToAccount(String toAccount);
    List<Transaction> findByFromAccountAndToAccount(String fromAccount, String toAccount);
}


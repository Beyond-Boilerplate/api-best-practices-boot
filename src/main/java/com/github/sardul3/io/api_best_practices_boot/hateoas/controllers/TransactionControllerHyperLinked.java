package com.github.sardul3.io.api_best_practices_boot.hateoas.controllers;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v3/transactions")
public class TransactionControllerHyperLinked {

    @GetMapping
    public ResponseEntity<EntityModel<Transaction>> getTransaction() {
        Transaction transaction = new Transaction();
        transaction.setFromAccount("ss");
        transaction.setToAccount("ss");
        EntityModel<Transaction> transactionModel = EntityModel.of(transaction);
        transactionModel.add(Link.of("https://myhost/transaction/42"));
        return ResponseEntity.ok(transactionModel);
    }


}

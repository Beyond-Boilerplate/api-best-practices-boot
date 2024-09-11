package com.github.sardul3.io.api_best_practices_boot.hateoas.models;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
public class TransactionModelAssembler extends RepresentationModelAssemblerSupport<Transaction, TransactionModel> {
    public TransactionModelAssembler() {
        super(TransactionModelAssembler.class, TransactionModel.class);
    }

    @Override
    public TransactionModel toModel(Transaction entity) {
        TransactionModel model = new TransactionModel();
        // Both CustomerModel and Customer have the same property names. So copy the values from the Entity to the Model
        model.from = entity.getFromAccount();
        model.to = entity.getToAccount();
        model.amount = entity.getAmount();
        return model;
    }
}

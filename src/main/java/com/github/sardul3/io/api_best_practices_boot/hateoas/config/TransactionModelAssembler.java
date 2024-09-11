package com.github.sardul3.io.api_best_practices_boot.hateoas.config;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.hateoas.models.TransactionModel;
import com.github.sardul3.io.api_best_practices_boot.pageFilterSort.controllers.TransactionsControllerPaged;
import org.springframework.beans.BeanUtils;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

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

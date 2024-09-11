package com.github.sardul3.io.api_best_practices_boot.hateoas.models;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;

public class TransactionModel extends RepresentationModel<TransactionModel> {
    public String from;
    public String to;
    public double amount;
}

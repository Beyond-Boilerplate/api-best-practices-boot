package com.github.sardul3.io.api_best_practices_boot.eTags.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * Entity class representing a transaction.
 * <p>
 * This class includes key fields such as fromAccount, toAccount, and amount, along with an enumerated status field.
 * The class is marked as Serializable to support caching and distributed environments.
 * </p>
 */
@Entity
@Getter
@Setter
public class Transaction implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    private String fromAccount;
    private String toAccount;
    private Double amount;

    /**
     * Enum representing the status of the transaction.
     * <p>
     * Possible statuses include:
     * <ul>
     *     <li>FLAGGED: The transaction has been flagged for review.</li>
     *     <li>ON_HOLD: The transaction is temporarily on hold.</li>
     *     <li>BLOCKED: The transaction has been blocked.</li>
     *     <li>COMPLETED: The transaction has been successfully completed.</li>
     *     <li>CANCELLED: The transaction has been cancelled.</li>
     * </ul>
     * </p>
     */
    public enum Status {
        FLAGGED, ON_HOLD, BLOCKED, COMPLETED, CANCELLED
    }

    @Enumerated(EnumType.STRING)
    private Status status;

}

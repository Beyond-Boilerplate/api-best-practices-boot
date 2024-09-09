package com.github.sardul3.io.api_best_practices_boot.eTags.config;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import io.micrometer.observation.annotation.Observed;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Utility class for generating eTags for transactions based on their state.
 * <p>
 * eTags are generated using the MD5 hash of specific transaction fields, ensuring
 * that each eTag uniquely identifies the state of a transaction or list of transactions.
 * </p>
 */
public class ETagGenerator {

    /**
     * Generates an eTag for a list of transactions by concatenating transaction IDs and
     * the total number of transactions. The concatenated string is hashed using MD5.
     *
     * @param transactions the list of transactions to generate the eTag for
     * @return the MD5 hash of the concatenated transaction data, used as the eTag
     */
    @Observed
    public static String generateETag(List<Transaction> transactions) {
        // Combine transaction IDs and the total number of transactions to ensure uniqueness
        String combinedTransactions = transactions.stream()
                .map(transaction -> transaction.getTransactionId() + transaction.getFromAccount() +
                        transaction.getToAccount() + transaction.getAmount() + transaction.getStatus().toString())
                .collect(Collectors.joining()) + transactions.size();

        // Generate MD5 hash of the combined string
        return getMD5Hash(combinedTransactions);
    }

    /**
     * Generates an eTag for a single transaction by concatenating its key fields (transactionId,
     * fromAccount, toAccount, amount, and status). The concatenated string is hashed using MD5.
     *
     * @param transaction the transaction to generate the eTag for
     * @return the MD5 hash of the concatenated transaction data, used as the eTag
     */
    public static String generateETagForTransaction(Transaction transaction) {
        // Concatenate important fields to ensure the eTag is unique to the transaction's state
        String transactionData = transaction.getTransactionId() + transaction.getFromAccount() +
                transaction.getToAccount() + transaction.getAmount() + transaction.getStatus();

        // Generate MD5 hash of the concatenated string
        return getMD5Hash(transactionData);
    }

    /**
     * Generates an MD5 hash from the given input string.
     *
     * @param input the input string to hash
     * @return the hexadecimal representation of the MD5 hash
     * @throws RuntimeException if the MD5 algorithm is not found
     */
    private static String getMD5Hash(String input) {
        try {
            // Get the MD5 digest instance
            MessageDigest md = MessageDigest.getInstance("MD5");

            // Calculate the digest (hash)
            byte[] digest = md.digest(input.getBytes());

            // Convert the byte array into a hex string
            return byteArrayToHexString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }

    /**
     * Converts a byte array into its corresponding hexadecimal string.
     *
     * @param bytes the byte array to convert
     * @return the hexadecimal string representation of the byte array
     */
    private static String byteArrayToHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}


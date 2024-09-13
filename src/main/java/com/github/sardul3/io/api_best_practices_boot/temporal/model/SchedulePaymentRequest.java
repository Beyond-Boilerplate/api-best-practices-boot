package com.github.sardul3.io.api_best_practices_boot.temporal.model;

import lombok.Data;

@Data
public class SchedulePaymentRequest {
    private String from;
    private String to;
    private double amount;
    private String when;
}

package com.github.sardul3.io.api_best_practices_boot.temporal.activities.validate;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.time.LocalDateTime;

@ActivityInterface
public interface SchedulePaymentActivities {

    @ActivityMethod
    void validateAmount(double amount);

    @ActivityMethod
    void createScheduledTask(String from, String to, double amount, LocalDateTime scheduledDate);

    @ActivityMethod
    Transaction runSchedulePayment(String from, String to, double amount, LocalDateTime scheduledDate);
}

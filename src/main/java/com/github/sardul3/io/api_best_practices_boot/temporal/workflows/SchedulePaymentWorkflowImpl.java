package com.github.sardul3.io.api_best_practices_boot.temporal.workflows;

import com.github.sardul3.io.api_best_practices_boot.eTags.models.Transaction;
import com.github.sardul3.io.api_best_practices_boot.temporal.activities.validate.SchedulePaymentActivities;
import java.time.Duration;

import com.github.sardul3.io.api_best_practices_boot.temporal.exception.TransactionCancelledException;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowException;
import io.temporal.workflow.Workflow;

import java.time.LocalDateTime;

/**
 * Implementation of the {@link SchedulePaymentWorkflow} for scheduling and executing a payment between accounts.
 * This workflow coordinates the scheduling and execution of payments through Temporal,
 * which ensures reliability, fault tolerance, and state persistence throughout the workflow's lifecycle.
 *
 * <p>Temporal workflows are designed to be deterministic and durable, meaning that they can be
 * paused, replayed, and resumed across different workers. This is especially useful for long-running,
 * stateful workflows that require timers or external service interactions (like scheduling tasks).
 *
 * <p>This workflow interacts with {@link SchedulePaymentActivities}, which are responsible for
 * performing the actual work, such as validating payment amounts, creating scheduled tasks, and
 * running the payment transaction.
 *
 * <p><b>Workflow Concepts:</b></p>
 * <ul>
 *     <li>**Workflows**: Define the orchestration logic. They are pure functions and are meant to be
 *     replayed deterministically to handle retries, crashes, and state recovery. Workflows themselves
 *     do not perform any external I/O or side effects; they delegate such tasks to Activities.</li>
 *
 *     <li>**Activities**: Activities perform external side effects such as calling databases or APIs.
 *     Activities are invoked from workflows using activity stubs. These activities are retried automatically
 *     in case of failures, and their results are persisted by Temporal.</li>
 * </ul>
 *
 * <p>This implementation uses {@link Workflow#newActivityStub(Class, ActivityOptions)} to create
 * an activity stub for invoking the {@link SchedulePaymentActivities}. The stub acts as a proxy to call
 * the activity methods from within the workflow in a way that Temporal can manage, retry, and persist.
 *
 * <p>Options like {@code setScheduleToCloseTimeout()} allow you to configure important properties
 * related to activity execution, such as timeouts and retry behavior.
 *
 * <p><b>ActivityOptions Explained:</b></p>
 * <ul>
 *     <li>**setScheduleToCloseTimeout**: Defines the maximum time allowed from the time the activity is scheduled
 *     to the time it must complete. If the activity takes longer than this duration, it will be automatically retried,
 *     and Temporal will handle the failure. In this case, we set it to 5 minutes.</li>
 *
 *     <li>**setStartToCloseTimeout**: Specifies the time from the start of the activity execution to its completion.
 *     This ensures that the activity itself finishes within a set amount of time, regardless of any scheduling delays.</li>
 *
 *     <li>**setRetryOptions**: Configures retry behavior for activities. You can set the maximum retry attempts,
 *     backoff intervals between retries, and non-retryable errors.</li>
 * </ul>
 *
 * <p><b>How This Workflow Works:</b></p>
 * <ol>
 *     <li>First, the payment amount is validated through {@link SchedulePaymentActivities#validateAmount(double)}.</li>
 *     <li>Then, the workflow calls {@link SchedulePaymentActivities#createScheduledTask(String, String, double, LocalDateTime)}
 *     to log or register the scheduled payment task.</li>
 *     <li>After scheduling the task, the workflow calculates the time delay between now and the specified scheduled
 *     date  and then sleeps until the scheduled time.</li>
 *     <li>Finally, once the scheduled time has passed, the workflow invokes
 *     {@link SchedulePaymentActivities#runSchedulePayment(String, String, double, LocalDateTime)} to execute the payment.</li>
 * </ol>
 */
public class SchedulePaymentWorkflowImpl implements SchedulePaymentWorkflow {

    private boolean isCancelled = false;

    // The Workflow.newActivityStub creates a proxy for the SchedulePaymentActivities interface,
    // allowing the workflow to invoke activities asynchronously. This proxy does not retrieve or
    // register all activities; instead, it defines how the specific activity interface (SchedulePaymentActivities)
    // will be invoked during the workflow's execution.
    //
    // When the workflow calls methods on this proxy, Temporal triggers the corresponding activity
    // externally (outside the workflow's context), ensuring that the activity is executed in a
    // separate, non-deterministic context. Temporal then manages the activity execution, handling
    // retries and timeouts as defined in the ActivityOptions.
    private final SchedulePaymentActivities activities = Workflow.newActivityStub(
            SchedulePaymentActivities.class,
            ActivityOptions.newBuilder()
                    .setScheduleToCloseTimeout(Duration.ofMinutes(5)) // Max time allowed from schedule to activity completion
                    .build()
    );

    /**
     * Orchestrates the scheduling and execution of a payment between accounts.
     *
     * @param from The account from which the payment will be deducted.
     * @param to The account to which the payment will be credited.
     * @param amount The amount to be transferred.
     * @param scheduledDate The date and time at which the payment should be executed.
     * @return A {@link Transaction} object representing the successful execution of the payment.
     */
    @Override
    public Transaction schedulePayment(String from, String to, double amount, LocalDateTime scheduledDate) {
        // Step 1: Validate the payment amount via an activity
        activities.validateAmount(amount);

        // Step 2: Create a scheduled task for logging or record purposes
        activities.createScheduledTask(from, to, amount, scheduledDate);

        // Step 3: Sleep until the scheduled time
        Duration delay = Duration.between(LocalDateTime.now(), scheduledDate);
        if (!delay.isNegative()) {
            Workflow.await(delay, () -> isCancelled);
//            Workflow.sleep(delay);
        }

        if(isCancelled) {
            throw new TransactionCancelledException();
        }

        // Step 4: Execute the payment once the scheduled time has arrived
        return activities.runSchedulePayment(from, to, amount, scheduledDate);
    }

    @Override
    public void cancelPayment() {
        this.isCancelled = true;
    }
}

package com.github.sardul3.io.api_best_practices_boot.temporal.service;

import com.github.sardul3.io.api_best_practices_boot.temporal.model.SchedulePaymentRequest;
import com.github.sardul3.io.api_best_practices_boot.temporal.model.ScheduledPaymentConfirmation;
import com.github.sardul3.io.api_best_practices_boot.temporal.workflows.SchedulePaymentWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Service
@AllArgsConstructor
public class SchedulePaymentService {

    private final WorkflowClient workflowClient;

    public ScheduledPaymentConfirmation buildAndStartWorkflow(SchedulePaymentRequest request, String correlationId) {
        String workflowId = computeWorkflowId(correlationId);
        LocalDateTime scheduledDateTime = convertDateTimeForSchedule(request.getWhen());
        SchedulePaymentWorkflow workflow = buildWorkflow(workflowId);
        WorkflowClient.start(workflow::schedulePayment, request.getFrom(), request.getTo(), request.getAmount(), scheduledDateTime);
        return buildConfirmation(workflowId);
    }

    private SchedulePaymentWorkflow buildWorkflow(String workflowId) {
        return workflowClient.newWorkflowStub(SchedulePaymentWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("ScheduledPaymentQueue")
                        .setWorkflowId(workflowId)
                        .build());
    }

    private String computeWorkflowId(String correlationId) {
        if(correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        return UUID.randomUUID().toString();
    }

    private LocalDateTime convertDateTimeForSchedule(String datetime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime scheduledDateTime;

        try {
            scheduledDateTime = LocalDateTime.parse(datetime, formatter);
        } catch (DateTimeParseException e) {
            // If date format is incorrect, set the time to 30 seconds from now
            scheduledDateTime = LocalDateTime.now().plusSeconds(30);
        }
        return scheduledDateTime;
    }

    private ScheduledPaymentConfirmation buildConfirmation(String workflowId) {
        ScheduledPaymentConfirmation confirmation = new ScheduledPaymentConfirmation();
        confirmation.setPaymentScheduleId(workflowId);
        return confirmation;
    }
}

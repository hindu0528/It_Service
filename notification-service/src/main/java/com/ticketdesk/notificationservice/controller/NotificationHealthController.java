package com.ticketdesk.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/notifications")
@CrossOrigin(origins = "*")
@Tag(name = "Notification Health Controller", description = "Endpoints for Notification Service status and event listener health")
public class NotificationHealthController {

    @GetMapping("/health")
    @Operation(summary = "Check Notification Service Health", description = "Returns service operational status.")
    public ResponseEntity<Map<String, Object>> getHealth() {
        return ResponseEntity.ok(Map.of(
                "service", "notification-service",
                "status", "UP",
                "kafkaConsumerGroup", "notification-group",
                "monitoredTopics", new String[]{"ticket.created", "ticket.status-changed", "ticket.comment-added"}
        ));
    }
}

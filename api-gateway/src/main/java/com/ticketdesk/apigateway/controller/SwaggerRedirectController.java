package com.ticketdesk.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class SwaggerRedirectController {

    @GetMapping({"/swagger-ui/index.html", "/swagger-ui", "/swagger-ui/"})
    public ResponseEntity<Void> redirectSwagger() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }
}

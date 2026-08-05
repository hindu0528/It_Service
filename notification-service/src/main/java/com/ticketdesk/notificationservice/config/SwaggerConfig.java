package com.ticketdesk.notificationservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TicketDesk Notification Service API")
                        .version("1.0")
                        .description("Asynchronous Kafka Event Consumer for Ticket Lifecycle Notifications"));
    }
}

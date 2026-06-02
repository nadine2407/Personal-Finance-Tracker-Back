package com.example.financetracker;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
    info = @Info(
        title = "Personal Finance Tracker API",
        version = "1.0.0",
        description = "API for managing personal finances: transactions, budgets, goals and accounts.",
        contact = @Contact(
            name = "Nadine MASROUR & Amina YOUS",
            email = "nad.masrour@gmail.com | amina.yous@dauphine.eu"
        )
    )
)
@SpringBootApplication
public class FinanceTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceTrackerApplication.class, args);
    }

}

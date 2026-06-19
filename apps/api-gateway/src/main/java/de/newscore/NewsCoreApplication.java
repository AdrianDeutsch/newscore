package de.newscore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the NewsCore GraphQL API gateway (BFF).
 *
 * <p>The gateway exposes a single GraphQL endpoint that aggregates editorial content for the
 * Nuxt frontend. See {@code docs/adr/ADR-002-graphql-bff-pattern.md} for the rationale.</p>
 */
@SpringBootApplication
public class NewsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsCoreApplication.class, args);
    }
}

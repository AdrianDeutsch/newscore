package de.newscore.integration;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.NewsCoreApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

/**
 * The default profile runs without any infrastructure, and the health endpoint has to say so. It
 * used to report DOWN (503) because it checked an ElasticSearch that this profile does not use.
 */
// Components are hidden by default, and a hidden component answers 404 whether it exists or not;
// showing them makes the assertions below able to fail.
@SpringBootTest(
        classes = NewsCoreApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "management.endpoint.health.show-components=always")
@Tag("integration")
class HealthIntegrationTest {

    @Autowired
    private TestRestTemplate http;

    @Test
    @DisplayName("without the elasticsearch profile the service is UP and does not check ElasticSearch")
    void defaultProfile_isUp() {
        ResponseEntity<String> health = http.getForEntity("/actuator/health", String.class);

        assertThat(health.getStatusCode().value()).isEqualTo(200);
        assertThat(health.getBody()).startsWith("{\"status\":\"UP\"").contains("\"diskSpace\"").doesNotContain("elasticsearch");
    }
}

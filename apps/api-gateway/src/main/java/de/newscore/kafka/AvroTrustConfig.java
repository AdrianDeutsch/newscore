package de.newscore.kafka;

import org.apache.avro.util.ClassSecurityValidator;
import org.apache.avro.util.ClassSecurityValidator.ClassSecurityPredicate;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Tells Avro which generated classes it may instantiate while (de)serializing (see ADR-011).
 *
 * <p>Since 1.12 Avro refuses to load any class named in a schema unless it is explicitly trusted,
 * to close deserialization-gadget attacks. Only the event classes generated from
 * {@code src/main/avro} are added here; everything else stays forbidden.</p>
 */
@Configuration
@Profile("kafka")
public class AvroTrustConfig {

    public AvroTrustConfig() {
        ClassSecurityValidator.setGlobal(trustedEventClasses());
    }

    /**
     * Builds the predicate: Avro's defaults plus the generated NewsCore event classes.
     *
     * @return the predicate to install as Avro's global validator
     */
    static ClassSecurityPredicate trustedEventClasses() {
        ClassSecurityPredicate events = ClassSecurityValidator.builder()
                .add(de.newscore.kafka.avro.ArticleEvent.class)
                .add(de.newscore.kafka.avro.ArticleEventType.class)
                .add(de.newscore.kafka.avro.SearchExecutedEvent.class)
                .add(de.newscore.kafka.avro.PageViewEvent.class)
                .build();
        return ClassSecurityValidator.composite(ClassSecurityValidator.DEFAULT, events);
    }
}

package de.newscore.unit.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import de.newscore.domain.Article;
import de.newscore.kafka.AvroTrustConfig;
import org.apache.avro.util.ClassSecurityValidator;
import org.apache.avro.util.ClassSecurityValidator.ClassSecurityPredicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AvroTrustConfigTest {

    private ClassSecurityPredicate previous;

    @BeforeEach
    void install() {
        previous = ClassSecurityValidator.getGlobal();
        new AvroTrustConfig();
    }

    @AfterEach
    void restore() {
        ClassSecurityValidator.setGlobal(previous);
    }

    @Test
    @DisplayName("the generated event classes are trusted")
    void generatedEventClasses_areTrusted() {
        ClassSecurityPredicate predicate = ClassSecurityValidator.getGlobal();

        assertThat(predicate.isTrusted(de.newscore.kafka.avro.ArticleEvent.class)).isTrue();
        assertThat(predicate.isTrusted(de.newscore.kafka.avro.ArticleEventType.class)).isTrue();
        assertThat(predicate.isTrusted(de.newscore.kafka.avro.SearchExecutedEvent.class)).isTrue();
        assertThat(predicate.isTrusted(de.newscore.kafka.avro.PageViewEvent.class)).isTrue();
    }

    @Test
    @DisplayName("any other application class stays forbidden")
    void otherClasses_stayForbidden() {
        ClassSecurityPredicate predicate = ClassSecurityValidator.getGlobal();

        assertThat(predicate.isTrusted(Article.class)).isFalse();
        assertThat(predicate.isTrusted(Runtime.class)).isFalse();
    }
}

package de.newscore.unit.cache;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import de.newscore.cache.HttpVarnishPurger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpVarnishPurgerTest {

    private static final String BASE_URL = "http://varnish:80";

    @Test
    @DisplayName("purgeArticle sends a PURGE to the article path")
    void purgeArticle_sendsPurgeRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/article/1"))
                .andExpect(method(HttpMethod.valueOf("PURGE")))
                .andRespond(withSuccess());
        HttpVarnishPurger purger = new HttpVarnishPurger(builder, BASE_URL);

        purger.purgeArticle("1");

        server.verify();
    }

    @Test
    @DisplayName("purge failures are swallowed and do not propagate")
    void purgeFailure_isSwallowed() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BASE_URL + "/")).andRespond(withServerError());
        HttpVarnishPurger purger = new HttpVarnishPurger(builder, BASE_URL);

        assertThatCode(purger::purgeHomepage).doesNotThrowAnyException();
        server.verify();
    }
}

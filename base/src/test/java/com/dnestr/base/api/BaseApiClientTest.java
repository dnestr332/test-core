package com.dnestr.base.api;

import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BaseApiClientTest {

    static class FakeApiClient extends BaseApiClient {
        @Override
        protected String getUrl() {
            return "https://api.example.com";
        }

        @Override
        protected String getPath() {
            return "/v1";
        }

        RequestSpecification bearer(String jwt) {
            return withBearer(jwt);
        }

        RequestSpecification basic(String user, String pass) {
            return withBasic(user, pass);
        }

        RequestSpecification apiKeyHeader(String name, String key) {
            return withApiKeyHeader(name, key);
        }

        RequestSpecification apiKeyQuery(String name, String key) {
            return withApiKeyQuery(name, key);
        }

        RequestSpecification oauth2(String token) {
            return withOAuth2(token);
        }

        RequestSpecification headers(Map<String, String> headers) {
            return withHeaders(headers);
        }
    }

    private final FakeApiClient client = new FakeApiClient();

    @Test
    void baseSpec_isNotNull() {
        assertThat(client.baseSpec()).isNotNull();
    }

    @Test
    void withBearer_buildsSpecWithoutError() {
        assertThat(client.bearer("token-123")).isNotNull();
    }

    @Test
    void withBasic_buildsSpecWithoutError() {
        assertThat(client.basic("user", "pass")).isNotNull();
    }

    @Test
    void withApiKeyHeader_buildsSpecWithoutError() {
        assertThat(client.apiKeyHeader("X-API-Key", "key-123")).isNotNull();
    }

    @Test
    void withApiKeyQuery_buildsSpecWithoutError() {
        assertThat(client.apiKeyQuery("apiKey", "key-123")).isNotNull();
    }

    @Test
    void withOAuth2_buildsSpecWithoutError() {
        assertThat(client.oauth2("token-123")).isNotNull();
    }

    @Test
    void withHeaders_buildsSpecWithoutError() {
        assertThat(client.headers(Map.of("X-Trace", "abc"))).isNotNull();
    }
}

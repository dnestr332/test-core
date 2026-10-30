package com.dnestr.base.api;

import io.restassured.filter.Filter;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseApiSpecTest {

    static class FakeApiSpec extends BaseApiSpec {
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

    private final FakeApiSpec spec = new FakeApiSpec();

    @Test
    void baseSpec_isNotNull() {
        assertThat(spec.baseSpec()).isNotNull();
    }

    @Test
    void baseSpec_logsRequestAndResponseOnlyIfValidationFails() {
        FilterableRequestSpecification built = (FilterableRequestSpecification) spec.baseSpec();

        assertThat(built.getConfig().getLogConfig().isLoggingOfRequestAndResponseIfValidationFailsEnabled()).isTrue();
    }

    @Test
    void baseSpec_hasNoExtraFiltersByDefault() {
        FilterableRequestSpecification built = (FilterableRequestSpecification) spec.baseSpec();

        assertThat(built.getDefinedFilters()).isEmpty();
    }

    @Test
    void baseSpec_attachesOverriddenFiltersInOrder() {
        Filter first = mock(Filter.class);
        Filter second = mock(Filter.class);
        FakeApiSpec withFilters = new FakeApiSpec() {
            @Override
            protected List<Filter> filters() {
                return List.of(first, second);
            }
        };

        FilterableRequestSpecification built = (FilterableRequestSpecification) withFilters.baseSpec();

        assertThat(built.getDefinedFilters()).containsExactly(first, second);
    }

    @Test
    void authHelpers_keepOverriddenFilters() {
        Filter filter = mock(Filter.class);
        FakeApiSpec withFilters = new FakeApiSpec() {
            @Override
            protected List<Filter> filters() {
                return List.of(filter);
            }
        };

        FilterableRequestSpecification built = (FilterableRequestSpecification) withFilters.bearer("token-123");

        assertThat(built.getDefinedFilters()).containsExactly(filter);
    }

    @Test
    void withBearer_buildsSpecWithoutError() {
        assertThat(spec.bearer("token-123")).isNotNull();
    }

    @Test
    void withBasic_buildsSpecWithoutError() {
        assertThat(spec.basic("user", "pass")).isNotNull();
    }

    @Test
    void withApiKeyHeader_buildsSpecWithoutError() {
        assertThat(spec.apiKeyHeader("X-API-Key", "key-123")).isNotNull();
    }

    @Test
    void withApiKeyQuery_buildsSpecWithoutError() {
        assertThat(spec.apiKeyQuery("apiKey", "key-123")).isNotNull();
    }

    @Test
    void withOAuth2_buildsSpecWithoutError() {
        assertThat(spec.oauth2("token-123")).isNotNull();
    }

    @Test
    void withHeaders_buildsSpecWithoutError() {
        assertThat(spec.headers(Map.of("X-Trace", "abc"))).isNotNull();
    }
}

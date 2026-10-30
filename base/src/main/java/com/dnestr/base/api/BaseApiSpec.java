package com.dnestr.base.api;

import io.restassured.RestAssured;
import io.restassured.config.LogConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.List;
import java.util.Map;

/**
 * Base for the REST-assured request configuration of a single service, meant to be extended (one
 * subclass per service/base-URL). Subclasses supply {@link #getUrl()}/{@link #getPath()} and expose
 * either {@link #baseSpec()} for unauthenticated calls or one of the {@code with*} helpers to layer a
 * specific auth scheme on top. This class only builds the {@link RequestSpecification}; sending the
 * request (endpoint, body, method) is left to the caller, typically a service class that injects
 * the subclass.
 * <p>
 * Every spec logs the full request and response when a {@code then()} check on it fails (and stays
 * quiet otherwise), so callers don't need {@code .log().ifValidationFails()} on each call. Extra
 * REST-assured filters, e.g. a report-attachment filter, are added by overriding {@link #filters()}.
 * To retry a call on {@code 429 Too Many Requests}, wrap it in {@link RateLimitRetry#onRateLimit}.
 */
public abstract class BaseApiSpec {

    /** The service's base URI, e.g. {@code https://api.example.com}. */
    protected abstract String getUrl();

    /** The base path appended to {@link #getUrl()} for every request built from this spec, e.g. {@code /v1}. */
    protected abstract String getPath();

    /**
     * REST-assured filters applied to every spec this class builds, in order. Empty by default.
     * Override to add e.g. a reporting filter such as Allure's {@code AllureRestAssured}, without
     * this library depending on Allure.
     */
    protected List<Filter> filters() {
        return List.of();
    }

    /**
     * A request spec pointed at {@link #getUrl()}/{@link #getPath()}, sending and expecting JSON,
     * with no authentication applied, request/response logging on validation failure, and
     * {@link #filters()} attached.
     */
    public RequestSpecification baseSpec() {
        RestAssuredConfig config = RestAssured.config()
                .logConfig(LogConfig.logConfig().enableLoggingOfRequestAndResponseIfValidationFails());
        return RestAssured.given()
                .config(config)
                .filters(filters())
                .baseUri(getUrl())
                .basePath(getPath())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON);
    }

    /** {@link #baseSpec()} with an {@code Authorization: Bearer <jwt>} header. */
    protected RequestSpecification withBearer(String jwt) {
        return baseSpec()
                .header("Authorization", "Bearer " + jwt);
    }

    /** {@link #baseSpec()} with preemptive HTTP Basic auth (credentials sent on the first request, not just after a 401 challenge). */
    protected RequestSpecification withBasic(String username, String password) {
        return baseSpec()
                .auth()
                .preemptive()
                .basic(username, password);
    }

    /** {@link #baseSpec()} with {@code apiKey} sent as the header named {@code headerName}. */
    protected RequestSpecification withApiKeyHeader(String headerName, String apiKey) {
        return baseSpec()
                .header(headerName, apiKey);
    }

    /** {@link #baseSpec()} with {@code apiKey} sent as the query parameter named {@code paramName}. */
    protected RequestSpecification withApiKeyQuery(String paramName, String apiKey) {
        return baseSpec()
                .queryParam(paramName, apiKey);
    }

    /** {@link #baseSpec()} with OAuth2 bearer auth via RestAssured's {@code auth().oauth2(token)}. */
    protected RequestSpecification withOAuth2(String token) {
        return baseSpec()
                .auth()
                .oauth2(token);
    }

    /** {@link #baseSpec()} with every entry of {@code headers} added as a request header. */
    protected RequestSpecification withHeaders(Map<String, String> headers) {
        return baseSpec()
                .headers(headers);
    }
}

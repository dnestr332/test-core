package com.dnestr.base.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

/**
 * Base for a REST-assured API client backing a single service, meant to be extended (one subclass
 * per service/base-URL). Subclasses supply {@link #getUrl()}/{@link #getPath()} and then either use
 * {@link #baseSpec()} directly for unauthenticated calls, or one of the {@code with*} helpers to
 * layer a specific auth scheme on top before issuing the request via RestAssured's fluent API.
 */
public abstract class BaseApiClient {

    /** The service's base URI, e.g. {@code https://api.example.com}. */
    protected abstract String getUrl();

    /** The base path appended to {@link #getUrl()} for every request from this client, e.g. {@code /v1}. */
    protected abstract String getPath();

    /** A request spec pointed at {@link #getUrl()}/{@link #getPath()}, sending and expecting JSON, with no authentication applied. */
    public RequestSpecification baseSpec() {
        return RestAssured.given()
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

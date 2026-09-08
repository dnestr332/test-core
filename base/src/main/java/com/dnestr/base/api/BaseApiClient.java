package com.dnestr.base.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import java.util.Map;

public abstract class BaseApiClient {

    protected abstract String getUrl();
    protected abstract String getPath();

    public RequestSpecification baseSpec() {
        return RestAssured.given()
                .baseUri(getUrl())
                .basePath(getPath())
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON);
    }

    protected RequestSpecification withBearer(String jwt) {
        return baseSpec()
                .header("Authorization", "Bearer " + jwt);
    }

    protected RequestSpecification withBasic(String username, String password) {
        return baseSpec()
                .auth()
                .preemptive()
                .basic(username, password);
    }

    protected RequestSpecification withApiKeyHeader(String headerName, String apiKey) {
        return baseSpec()
                .header(headerName, apiKey);
    }

    protected RequestSpecification withApiKeyQuery(String paramName, String apiKey) {
        return baseSpec()
                .queryParam(paramName, apiKey);
    }

    protected RequestSpecification withOAuth2(String token) {
        return baseSpec()
                .auth()
                .oauth2(token);
    }

    protected RequestSpecification withHeaders(Map<String, String> headers) {
        return baseSpec()
                .headers(headers);
    }
}

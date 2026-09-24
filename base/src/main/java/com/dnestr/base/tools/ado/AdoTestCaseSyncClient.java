package com.dnestr.base.tools.ado;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Raw REST client for the Azure DevOps Test Plans and Work Item Tracking APIs (api-version 7.1),
 * used by {@link AdoSyncProvider} to create/update test cases, manage sprint suites, and link test
 * cases to PBIs. Every call funnels through {@link #requireOk} and throws {@link IllegalStateException}
 * on a non-2xx response — there's no retry or partial-failure handling here; a failed call fails
 * the whole {@link AdoSyncProvider#sync} call for that scenario (caught and recorded by
 * {@code BddSyncEngine}, not swallowed here).
 */
@RequiredArgsConstructor
public final class AdoTestCaseSyncClient {

    private static final String API_VERSION = "7.1";
    private static final String TEST_CASE_PATH = "$Test%20Case";
    private static final String TITLE_FIELD = "System.Title";
    private static final String STEPS_FIELD = "Microsoft.VSTS.TCM.Steps";
    private static final String RELATED_LINK = "System.LinkTypes.Related";

    private final RequestSpecification specification;
    private final AdoSyncConfig config;

    private RequestSpecification request() {
        return RestAssured.given().spec(specification);
    }

    /** Returns the ID of the static test suite named {@code sprint} under the configured parent suite, creating it first if it doesn't exist yet. */
    public int resolveOrCreateSprintSuite(String sprint) {
        return findSprintSuite(sprint)
                .orElseGet(() -> createSprintSuite(sprint));
    }

    /** Creates a new ADO "Test Case" work item with {@code title} and a single action step built from {@code gherkinBody}, returning its work item ID. */
    public long createTestCase(String title, String gherkinBody) {
        List<Map<String, Object>> patch = List.of(
                patchField(TITLE_FIELD, title),
                patchField(STEPS_FIELD, toAdoSteps(gherkinBody))
        );

        Response response = patchRequest()
                .body(patch)
                .post("/%s/_apis/wit/workitems/%s?api-version=%s"
                        .formatted(config.project(), TEST_CASE_PATH, API_VERSION)
                );

        requireOk(response, "create test case '" + title + "'");

        return response.jsonPath().getLong("id");
    }

    /** Adds an existing test case to a test suite within the configured test plan. */
    public void addTestCaseToSuite(int suiteId, long testCaseId) {
        Response response = request()
                .post("/%s/_apis/test/Plans/%d/suites/%d/testcases/%d?api-version=%s"
                        .formatted(
                                config.project(),
                                config.planId(),
                                suiteId,
                                testCaseId,
                                API_VERSION
                        )
                );

        requireOk(response, "add test case " + testCaseId + " to suite " + suiteId);
    }

    /** Overwrites the title and action step of an existing test case's work item with {@code title}/{@code gherkinBody}. */
    public void updateTestCase(long testCaseId, String title, String gherkinBody) {
        List<Map<String, Object>> patch = List.of(
                patchField(TITLE_FIELD, title),
                patchField(STEPS_FIELD, toAdoSteps(gherkinBody))
        );

        Response response = patchRequest()
                .body(patch)
                .patch("/%s/_apis/wit/workitems/%d?api-version=%s"
                        .formatted(config.project(), testCaseId, API_VERSION)
                );

        requireOk(response, "update test case " + testCaseId);
    }

    /** Adds a "Related" work item link from the test case to the PBI, unless that link already exists (checked via {@link #hasPbiLink}). */
    public void linkTestCaseToPbi(long testCaseId, long pbiId) {
        String pbiUrl = workItemUrl(pbiId);

        if (hasPbiLink(testCaseId, pbiUrl)) return;

        Map<String, Object> relation = Map.of(
                "rel", RELATED_LINK,
                "url", pbiUrl,
                "attributes", Map.of(
                        "comment", "Linked by BDD sync"
                )
        );

        List<Map<String, Object>> patch = List.of(
                Map.of("op", "add",
                        "path", "/relations/-",
                        "value", relation)
        );

        Response response = patchRequest()
                .body(patch)
                .patch("/%s/_apis/wit/workitems/%d?api-version=%s"
                        .formatted(config.project(), testCaseId, API_VERSION)
                );

        requireOk(response, "link test case " + testCaseId + " to PBI " + pbiId);
    }

    private Optional<Integer> findSprintSuite(String sprint) {
        Response response = request()
                .get("/%s/_apis/testplan/Plans/%d/suites"
                        .formatted(config.project(), config.planId())
                        + "?api-version=" + API_VERSION
                );

        requireOk(response, "list suites");

        List<Map<String, Object>> suites = response.jsonPath().getList("value");

        return suites.stream()
                .filter(suite -> sprint.equals(suite.get("name")))
                .filter(suite -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parent =
                            (Map<String, Object>) suite.get("parentSuite");

                    return parent != null
                            && parent.get("id") instanceof Number id
                            && id.intValue() == config.parentSuiteId();
                })
                .map(suite -> ((Number) suite.get("id")).intValue())
                .findFirst();
    }

    private int createSprintSuite(String sprint) {
        Map<String, Object> body = Map.of(
                "suiteType", "staticTestSuite",
                "name", sprint,
                "parentSuite", Map.of("id", config.parentSuiteId())
        );

        Response response = request()
                .body(body)
                .post("/%s/_apis/testplan/Plans/%d/suites?api-version=%s"
                        .formatted(config.project(), config.planId(), API_VERSION)
                );

        requireOk(response, "create sprint suite " + sprint);

        return response.jsonPath().getInt("id");
    }

    private void requireOk(Response response, String call) {
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("ADO API call '" + call + "' failed ("
                    + response.statusCode() + "): " + response.asString());
        }
    }

    private RequestSpecification patchRequest() {
        return request().contentType("application/json-patch+json");
    }

    private Map<String, Object> patchField(String field, Object value) {
        return Map.of(
                "op", "add",
                "path", "/fields/" + field,
                "value", value
        );
    }

    private String toAdoSteps(String gherkinBody) {
        String content = escapeXml(gherkinBody);

        return """
            <steps id="0" last="1">
                <step id="1" type="ActionStep">
                    <parameterizedString isformatted="true">%s</parameterizedString>
                    <parameterizedString isformatted="true"></parameterizedString>
                    <description/>
                </step>
            </steps>
            """.formatted(content);
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String workItemUrl(long workItemId) {
        Response response = request().get(
                "/%s/_apis/wit/workitems/%d?api-version=%s"
                        .formatted(config.project(), workItemId, API_VERSION)
        );
        requireOk(response, "get work item " + workItemId);

        return response.jsonPath().getString("url");
    }

    /** Whether the test case already has a "Related" link to the PBI. */
    public boolean hasPbiLink(long testCaseId, long pbiId) {
        return hasPbiLink(testCaseId, workItemUrl(pbiId));
    }

    private boolean hasPbiLink(long testCaseId, String expectedPbiUrl) {
        Response response = request()
                .get("/%s/_apis/wit/workitems/%d?$expand=relations&api-version=%s"
                        .formatted(config.project(), testCaseId, API_VERSION)
                );

        requireOk(response, "get test case relations " + testCaseId);

        List<Map<String, Object>> relations = response.jsonPath().getList("relations");
        if (relations == null) return false;

        return relations.stream()
                .anyMatch(relation ->
                        RELATED_LINK.equals(relation.get("rel"))
                                && expectedPbiUrl.equals(relation.get("url"))
                );
    }
}

package com.dnestr.base.tools.testrail;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Raw REST client for the TestRail v2 API, used by {@link TestRailSyncProvider} to create/update
 * cases, resolve project-specific configuration (template and custom field IDs, which vary per
 * TestRail instance so are looked up by hint rather than hardcoded), and manage per-feature
 * subsections. Every call funnels through {@link #requireOk} and throws {@link IllegalStateException}
 * on a non-2xx response, with no retry — a failed call fails the whole
 * {@link TestRailSyncProvider#sync} call for that scenario.
 */
@Slf4j
public class TestRailCaseSyncClient {

    private final RequestSpecification specification;
    private List<Map<String, Object>> caseFieldsCache;

    public TestRailCaseSyncClient(RequestSpecification specification) {
        this.specification = specification;
    }

    private RequestSpecification request() {
        return io.restassured.RestAssured.given()
                .spec(specification);
    }

    /** Returns the ID of the project that owns {@code suiteId}. */
    public int resolveProjectId(int suiteId) {
        Response response = request().get("/index.php?/api/v2/get_suite/" + suiteId);
        requireOk(response, "get_suite/" + suiteId);
        return response.jsonPath().getInt("project_id");
    }

    /**
     * Returns the ID of {@code projectId}'s Gherkin/BDD case template, matched by name against
     * {@code "gherkin"}/{@code "bdd"}/{@code "behaviour driven"}/{@code "behavior driven"} — the
     * exact template name isn't standardized across TestRail instances, so this matches loosely
     * rather than by a fixed name.
     *
     * @throws IllegalStateException if no template name matches any of those hints
     */
    public int resolveGherkinTemplateId(int projectId) {
        Response response = request().get("/index.php?/api/v2/get_templates/" + projectId);
        requireOk(response, "get_templates/" + projectId);

        List<Map<String, Object>> templates = response.jsonPath().getList("$");
        return templates.stream()
                .filter(t -> matches((String) t.get("name"), "gherkin", "bdd", "behaviour driven", "behavior driven"))
                .findFirst()
                .map(t -> (Integer) t.get("id"))
                .orElseThrow(() -> new IllegalStateException(
                        "No Gherkin/BDD template was found on project " + projectId
                                + ". Templates found: " + templates.stream().map(t -> t.get("name")).toList()
                                + " - confirm the exact template name and adjust resolveGherkinTemplateId()."
                ));
    }

    /**
     * Finds a custom case field by matching any of the given hints against its system_name or label.
     * Returns empty if nothing matches - callers decide whether that's fatal.
     */
    public Optional<CaseField> resolveField(String... hints) {
        for (Map<String, Object> field : caseFields()) {
            String systemName = String.valueOf(field.get("system_name"));
            String label = String.valueOf(field.get("label"));
            if (matches(systemName, hints) || matches(label, hints)) {
                return Optional.of(new CaseField(systemName, label, (Integer) field.get("type_id")));
            }
        }
        return Optional.empty();
    }

    /**
     * Same as {@link #resolveField}, but throws with the full list of available fields
     * (system_name + label) when nothing matches - so a bad hint list is diagnosable from the
     * error alone instead of needing another round trip.
     */
    public CaseField resolveRequiredField(String... hints) {
        return resolveField(hints).orElseThrow(() -> new IllegalStateException(
                "No case field matched hints " + List.of(hints) + ". Fields found: "
                        + caseFields().stream()
                        .map(f -> f.get("system_name") + " (label '" + f.get("label") + "')")
                        .toList()
                        + " - confirm the exact field and adjust the hints passed to resolveRequiredField()."
        ));
    }

    private List<Map<String, Object>> caseFields() {
        if (caseFieldsCache == null) {
            Response response = request().get("/index.php?/api/v2/get_case_fields");
            requireOk(response, "get_case_fields");
            caseFieldsCache = response.jsonPath().getList("$");
        }
        return caseFieldsCache;
    }

    /** Creates a new case in {@code sectionId} with {@code title}/{@code templateId}/{@code refs} plus every entry of {@code customFields}, returning its new case ID. */
    public long createCase(int sectionId, String title, int templateId, String refs, Map<String, Object> customFields) {
        Map<String, Object> body = getPostBody(title, templateId, refs, customFields);

        Response response = request()
                .body(body)
                .post("/index.php?/api/v2/add_case/" + sectionId);
        requireOk(response, "add_case/" + sectionId);
        log.info("add_case raw response: {}", response.asString());

        long caseId = response.jsonPath().getLong("id");
        log.info("Created TestRail case C{} - '{}'", caseId, title);
        return caseId;
    }

    /** Overwrites {@code caseId}'s title/template/refs plus every entry of {@code customFields}. */
    public void updateCase(long caseId, String title, int templateId, String refs, Map<String, Object> customFields) {
        Map<String, Object> body = getPostBody(title, templateId, refs, customFields);

        Response response = request()
                .body(body)
                .post("/index.php?/api/v2/update_case/" + caseId);
        requireOk(response, "update_case/" + caseId);

        log.info("Updated TestRail case C{} - '{}'", caseId, title);
    }

    /**
     * Finds an existing subsection named {@code name} directly under {@code parentSectionId},
     * or creates one if none exists yet - so re-running the sync for the same feature file
     * reuses the same subsection instead of creating a duplicate every time.
     */
    public int resolveOrCreateSubsection(int projectId, int suiteId, int parentSectionId, String name) {
        for (Map<String, Object> section : listSections(projectId, suiteId)) {
            Object parentId = section.get("parent_id");
            boolean sameParent = parentId instanceof Number n && n.intValue() == parentSectionId;
            if (sameParent && name.equals(section.get("name"))) {
                return ((Number) section.get("id")).intValue();
            }
        }

        Map<String, Object> body = Map.of("name", name, "suite_id", suiteId, "parent_id", parentSectionId);
        Response response = request()
                .body(body)
                .post("/index.php?/api/v2/add_section/" + projectId);
        requireOk(response, "add_section/" + projectId);

        int newId = response.jsonPath().getInt("id");
        log.info("Created TestRail subsection '{}' (id {}) under parent section {}", name, newId, parentSectionId);
        return newId;
    }

    private List<Map<String, Object>> listSections(int projectId, int suiteId) {
        Response response = request()
                .get("/index.php?/api/v2/get_sections/" + projectId + "&suite_id=" + suiteId);
        requireOk(response, "get_sections/" + projectId);

        List<Map<String, Object>> sections = response.jsonPath().getList("sections");
        return sections != null ? sections : response.jsonPath().getList("$");
    }

    /** Fetches a case's full raw field map, e.g. to verify a just-written custom field actually stuck. */
    public Map<String, Object> getCase(long caseId) {
        Response response = request()
                .get("/index.php?/api/v2/get_case/" + caseId);
        requireOk(response, "get_case/" + caseId);
        return response.jsonPath().getMap("$");
    }

    private boolean matches(String value, String... hints) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase();
        for (String hint : hints) {
            if (lower.contains(hint.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private void requireOk(Response response, String call) {
        if (response.statusCode() >= 300) {
            throw new IllegalStateException("TestRail API call '" + call + "' failed ("
                    + response.statusCode() + "): " + response.asString());
        }
    }

    private Map<String, Object> getPostBody(String title, int templateId, String refs, Map<String, Object> customFields) {
        Map<String, Object> body = new java.util.HashMap<>(customFields);
        body.put("title", title);
        body.put("template_id", templateId);
        if (refs != null && !refs.isBlank()) {
            body.put("refs", refs);
        }
        return body;
    }

    /** A resolved TestRail custom case field, as found by {@link #resolveField}/{@link #resolveRequiredField}. */
    public record CaseField(String systemName, String label, Integer typeId) {
    }
}

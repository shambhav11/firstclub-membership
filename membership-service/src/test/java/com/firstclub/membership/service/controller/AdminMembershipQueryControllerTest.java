package com.firstclub.membership.service.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
class AdminMembershipQueryControllerTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void returnsAnEmptyPaginatedTierList() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/membership-tiers").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void returnsAnEmptyPaginatedPerkList() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/membership-perks").param("page", "0").param("size", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.totalElements").value(0));
  }

  @Test
  void returnsAStandardNotFoundResponseForUnknownTier() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/membership-tiers/{tierId}", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void returnsAStandardNotFoundResponseForUnknownDetailedTierSummary() throws Exception {
    mockMvc
        .perform(get("/api/v1/admin/membership-tiers/{tierId}/summary", UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
  }

  @Test
  void returnsFieldViolationsForAnInvalidRequestBody() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/membership-tiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").value("One or more request fields are invalid"))
        .andExpect(
            jsonPath(
                "$.fieldViolations[*].field",
                hasItems("displayName", "initialPlans", "requestedBy")));
  }

  @Test
  @Transactional
  void returnsACompactTierSummaryAndDisplayReadyPerkTierReferences() throws Exception {
    UUID tierId = UUID.randomUUID();
    UUID perkId = UUID.randomUUID();
    Instant now = Instant.now();
    insertTier(tierId, "Gold", now);
    insertPlan(UUID.randomUUID(), tierId, now);
    insertPerk(perkId, now);
    jdbcTemplate.update(
        "insert into membership_tier_perks (id, membership_tier_id, membership_perk_id, created_at)"
            + " values (?, ?, ?, ?)",
        UUID.randomUUID(),
        tierId,
        perkId,
        now);

    mockMvc
        .perform(get("/api/v1/admin/membership-tiers/{tierId}/summary", tierId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.displayName").value("Gold"))
        .andExpect(jsonPath("$.plans[0].durationDays").value(30))
        .andExpect(jsonPath("$.perks[0].id").value(perkId.toString()))
        .andExpect(jsonPath("$.perks[0].displayText[0].text").value("Free delivery"))
        .andExpect(jsonPath("$.perks[0].appliesTo").value("DELIVERY"))
        .andExpect(jsonPath("$.perks[0].tiers").doesNotExist());

    mockMvc
        .perform(get("/api/v1/admin/membership-perks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(perkId.toString()))
        .andExpect(jsonPath("$.content[0].tiers[0].id").value(tierId.toString()))
        .andExpect(jsonPath("$.content[0].tiers[0].displayName").value("Gold"))
        .andExpect(jsonPath("$.content[0].tierIds").doesNotExist());
  }

  @Test
  void rollsBackAllWritesWhenTierCreationHasDuplicatePlanDurations() throws Exception {
    String request =
        """
{
  "displayName": "Rollback Tier",
  "enrollmentClosed": false,
  "initialPlans": [
    {"durationDays": 30, "displayText": "Monthly", "amount": 199, "currencyCode": "INR", "enrollmentClosed": false},
    {"durationDays": 30, "displayText": "Another monthly", "amount": 299, "currencyCode": "INR", "enrollmentClosed": false}
  ],
  "perks": [
    {"displayText": [{"text": "Free delivery", "bold": true}], "appliesTo": "DELIVERY", "perkApplication": {"applicationType":"DELIVERY", "discountPercent":100, "minOrderValue":0, "currencyCode":"INR"}}
  ],
  "requestedBy": "test-admin"
}
""";

    mockMvc
        .perform(
            post("/api/v1/admin/membership-tiers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CONFLICT"));

    org.junit.jupiter.api.Assertions.assertEquals(0, count("membership_tiers"));
    org.junit.jupiter.api.Assertions.assertEquals(0, count("membership_plans"));
    org.junit.jupiter.api.Assertions.assertEquals(0, count("membership_perks"));
    org.junit.jupiter.api.Assertions.assertEquals(0, count("membership_tier_perks"));
  }

  @Test
  @Transactional
  void completesTheAdminTierPlanAndPerkFlowEndToEnd() throws Exception {
    clearMembershipData();
    JsonNode silver =
        postJson(
            "/api/v1/admin/membership-tiers",
            """
{"displayName":"Silver","enrollmentClosed":false,
 "initialPlans":[{"durationDays":30,"displayText":"Monthly","amount":199,"currencyCode":"INR","enrollmentClosed":false}],
 "perks":[{"displayText":[{"text":"Free delivery","bold":true}],"appliesTo":"DELIVERY","perkApplication":{"applicationType":"DELIVERY","discountPercent":100,"minOrderValue":0,"currencyCode":"INR"}}]}
""");
    JsonNode gold =
        postJson(
            "/api/v1/admin/membership-tiers",
            """
{"displayName":"Gold","enrollmentClosed":false,
 "initialPlans":[{"durationDays":30,"displayText":"Monthly","amount":299,"currencyCode":"INR","enrollmentClosed":false}],"perks":[]}
""");
    String silverId = silver.path("id").asText();
    String goldId = gold.path("id").asText();
    String initialPerkId = silver.path("perks").get(0).path("id").asText();

    postJson(
        "/api/v1/admin/membership-plans",
        """
{"membershipTierId":"%s","durationDays":90,"displayText":"Quarterly","amount":499,"currencyCode":"INR","enrollmentClosed":false}
"""
            .formatted(silverId));
    JsonNode priorityPerk =
        postJson(
            "/api/v1/admin/membership-perks",
            """
{"displayText":[{"text":"Priority support","bold":false}],"appliesTo":"SUPPORT","perkApplication":{"applicationType":"SUPPORT","callbackWithinUnit":"MINUTES","callbackWithinValue":10}}
""");
    String priorityPerkId = priorityPerk.path("id").asText();

    addPerk(silverId, priorityPerkId);
    addPerk(silverId, priorityPerkId);
    addPerk(goldId, priorityPerkId);
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/membership-perks/{perkId}/status", priorityPerkId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ARCHIVED\",\"requestedBy\":\"test-admin\"}"))
        .andExpect(status().isConflict());
    removePerk(silverId, initialPerkId);
    removePerk(silverId, initialPerkId);
    mockMvc
        .perform(
            post("/api/v1/admin/membership-tiers/{tierId}/perks", goldId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"membershipPerkIds\":[\"%s\"],\"requestedBy\":\"test-admin\"}"
                        .formatted(initialPerkId)))
        .andExpect(status().isConflict());

    mockMvc
        .perform(get("/api/v1/admin/membership-tiers/{tierId}/summary", silverId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plans.length()").value(2))
        .andExpect(jsonPath("$.perks.length()").value(1))
        .andExpect(jsonPath("$.perks[0].id").value(priorityPerkId));
    mockMvc
        .perform(get("/api/v1/admin/membership-tiers/{tierId}/summary", goldId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.plans.length()").value(1))
        .andExpect(jsonPath("$.perks[0].id").value(priorityPerkId));
    mockMvc
        .perform(
            get("/api/v1/admin/membership-perks")
                .param("status", "ARCHIVED")
                .param("search", "free delivery"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(initialPerkId));
  }

  @Test
  @Transactional
  void persistsEnrollmentClosedOnCreateAndTierAndPlanUpdates() throws Exception {
    clearMembershipData();
    JsonNode tier =
        postJson(
            "/api/v1/admin/membership-tiers",
            """
{"displayName":"Enrollment control","enrollmentClosed":true,
 "initialPlans":[{"durationDays":30,"displayText":"Monthly","amount":199,"currencyCode":"INR","enrollmentClosed":true}],"perks":[]}
""");
    String tierId = tier.path("id").asText();
    String initialPlanId = tier.path("plans").get(0).path("id").asText();
    org.junit.jupiter.api.Assertions.assertTrue(tier.path("enrollmentClosed").asBoolean());
    org.junit.jupiter.api.Assertions.assertTrue(
        tier.path("plans").get(0).path("enrollmentClosed").asBoolean());

    JsonNode updatedTier =
        putJson(
            "/api/v1/admin/membership-tiers/" + tierId,
            """
            {"displayName":"Enrollment control updated","status":"ACTIVE","enrollmentClosed":false}
            """);
    org.junit.jupiter.api.Assertions.assertFalse(updatedTier.path("enrollmentClosed").asBoolean());
    JsonNode updatedPlan =
        putJson(
            "/api/v1/admin/membership-plans/" + initialPlanId,
            """
{"displayText":"Monthly updated","amount":249,"currencyCode":"INR","status":"INACTIVE","enrollmentClosed":false}
""");
    org.junit.jupiter.api.Assertions.assertFalse(updatedPlan.path("enrollmentClosed").asBoolean());
    org.junit.jupiter.api.Assertions.assertEquals("INACTIVE", updatedPlan.path("status").asText());
  }

  @Test
  @Transactional
  void persistsInternalAuditActorsOnTierCreateAndUpdate() throws Exception {
    clearMembershipData();
    JsonNode tier =
        postJsonAs(
            "/api/v1/admin/membership-tiers",
            """
{"displayName":"Audit tier","enrollmentClosed":false,
 "initialPlans":[{"durationDays":30,"displayText":"Monthly","amount":199,"currencyCode":"INR","enrollmentClosed":false}],"perks":[]}
""",
            "creator@example.com");
    String tierId = tier.path("id").asText();
    String planId = tier.path("plans").get(0).path("id").asText();

    assertAuditActors("membership_tiers", tierId, "creator@example.com", "creator@example.com");
    assertAuditActors("membership_plans", planId, "creator@example.com", "creator@example.com");

    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                    "/api/v1/admin/membership-tiers/{tierId}", tierId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
{"displayName":"Audit tier updated","status":"ACTIVE","enrollmentClosed":false,"requestedBy":"updater@example.com"}
"""))
        .andExpect(status().isOk());

    assertAuditActors("membership_tiers", tierId, "creator@example.com", "updater@example.com");
  }

  private JsonNode postJson(String path, String request) throws Exception {
    return postJsonAs(path, request, "test-admin");
  }

  private JsonNode postJsonAs(String path, String request, String requestedBy) throws Exception {
    String content =
        mockMvc
            .perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(withRequestedBy(request, requestedBy)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(content);
  }

  private JsonNode putJson(String path, String request) throws Exception {
    String content =
        mockMvc
            .perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(withRequestedBy(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return objectMapper.readTree(content);
  }

  private void addPerk(String tierId, String perkId) throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/membership-tiers/{tierId}/perks", tierId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"membershipPerkIds\":[\"%s\"],\"requestedBy\":\"test-admin\"}"
                        .formatted(perkId)))
        .andExpect(status().isNoContent());
  }

  private void removePerk(String tierId, String perkId) throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/admin/membership-tiers/{tierId}/perks", tierId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"membershipPerkIds\":[\"%s\"],\"requestedBy\":\"test-admin\"}"
                        .formatted(perkId)))
        .andExpect(status().isNoContent());
  }

  private String withRequestedBy(String request) throws Exception {
    return withRequestedBy(request, "test-admin");
  }

  private String withRequestedBy(String request, String requestedBy) throws Exception {
    com.fasterxml.jackson.databind.node.ObjectNode body =
        (com.fasterxml.jackson.databind.node.ObjectNode) objectMapper.readTree(request);
    body.put("requestedBy", requestedBy);
    return objectMapper.writeValueAsString(body);
  }

  private void clearMembershipData() {
    jdbcTemplate.update("delete from membership_tier_perks");
    jdbcTemplate.update("delete from membership_plans");
    jdbcTemplate.update("delete from membership_perks");
    jdbcTemplate.update("delete from membership_tiers");
  }

  private int count(String tableName) {
    return jdbcTemplate.queryForObject("select count(*) from " + tableName, Integer.class);
  }

  private void assertAuditActors(
      String tableName, String id, String createdBy, String updatedBy) {
    java.util.Map<String, Object> auditActors =
        jdbcTemplate.queryForMap(
            "select created_by, updated_by from " + tableName + " where id = ?",
            UUID.fromString(id));
    org.junit.jupiter.api.Assertions.assertEquals(createdBy, auditActors.get("created_by"));
    org.junit.jupiter.api.Assertions.assertEquals(updatedBy, auditActors.get("updated_by"));
  }

  private void insertTier(UUID tierId, String displayName, Instant now) {
    jdbcTemplate.update(
        "insert into membership_tiers (id, display_name, status, enrollment_closed, created_at,"
            + " updated_at) values (?, ?, ?, ?, ?, ?)",
        tierId,
        displayName,
        "ACTIVE",
        false,
        now,
        now);
  }

  private void insertPlan(UUID planId, UUID tierId, Instant now) {
    jdbcTemplate.update(
        "insert into membership_plans (id, membership_tier_id, duration_days, display_text, amount,"
            + " currency_code, status, enrollment_closed, created_at, updated_at) values (?, ?, ?,"
            + " ?, ?, ?, ?, ?, ?, ?)",
        planId,
        tierId,
        30,
        "Monthly",
        BigDecimal.valueOf(199),
        "INR",
        "ACTIVE",
        false,
        now,
        now);
  }

  private void insertPerk(UUID perkId, Instant now) {
    jdbcTemplate.update(
        "insert into membership_perks (id, display_text, display_text_key, applies_to,"
            + " perk_application, status, created_at, updated_at) values (?, ? format json, ?, ?, ?"
            + " format json, ?, ?, ?)",
        perkId,
        "[{\"text\":\"Free delivery\",\"bold\":true}]",
        "free delivery",
        "DELIVERY",
        "{\"applicationType\":\"DELIVERY\",\"discountPercent\":100,\"minOrderValue\":0,\"currencyCode\":\"INR\"}",
        "ACTIVE",
        now,
        now);
  }
}

package com.firstclub.membership.service.controller;

import com.firstclub.membership.core.query.UserMembershipCatalogManager;
import com.firstclub.membership.core.query.UserMembershipQueryManager;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.PageResponse;
import com.firstclub.membership.models.response.user.MembershipCatalogResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import com.firstclub.membership.models.response.user.UserMembershipPerkResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@Tag(
    name = "User membership reads",
    description = "Current membership, history, catalog, and operational-perk reads.")
public class UserMembershipQueryController {
  private final UserMembershipCatalogManager userMembershipCatalogManager;
  private final UserMembershipQueryManager userMembershipQueryManager;

  public UserMembershipQueryController(
      UserMembershipCatalogManager catalogManager, UserMembershipQueryManager queryManager) {
    this.userMembershipCatalogManager = catalogManager;
    this.userMembershipQueryManager = queryManager;
  }

  @GetMapping("/api/v1/membership-options")
  @Operation(
      summary = "Get purchasable membership options",
      description =
          "Returns every ACTIVE, enrollment-open tier with its ACTIVE, enrollment-open plans and"
              + " ACTIVE mapped benefits. This is the catalog to present before a user starts"
              + " checkout.")
  @ApiResponse(
      responseCode = "200",
      description = "Available tiers, plans, and display-ready benefits.")
  public MembershipCatalogResponse getMembershipOptions() {
    return userMembershipCatalogManager.getAvailableMembershipOptions();
  }

  @GetMapping("/api/v1/users/{userId}/memberships/current")
  @Operation(
      summary = "Get a user's current active membership",
      description =
          "Returns the active subscription with its tier, plan, latest payment, and refund summary."
              + " Use history when the user has no active membership or needs prior transitions.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Current active membership."),
    @ApiResponse(
        responseCode = "404",
        description = "User does not exist or has no active membership.")
  })
  public MembershipSubscriptionResponse getCurrent(@PathVariable java.util.UUID userId) {
    return userMembershipQueryManager.getCurrent(userId);
  }

  @GetMapping("/api/v1/users/{userId}/memberships/perks")
  @Operation(
      summary = "Get typed perks unlocked by a user's active membership",
      description =
          "Returns active operational perks for the active membership's tier. The `perkApplication`"
              + " is the typed configuration consumers use at checkout/catalog time—for example"
              + " delivery thresholds, eligible cart verticals, sale-access hours, deal tiers, or"
              + " support callback SLA. A valid user without an active membership receives an empty"
              + " array.")
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Active typed perks; possibly an empty array."),
    @ApiResponse(
        responseCode = "400",
        description = "An appliesTo value is not a supported perk area."),
    @ApiResponse(responseCode = "404", description = "User does not exist.")
  })
  public List<UserMembershipPerkResponse> getActivePerks(
      @PathVariable java.util.UUID userId,
      @Parameter(
              description =
                  "Optional repeatable filter. For example: `?appliesTo=DELIVERY&appliesTo=CART`. "
                      + "Omit to return all active perks.",
              example = "DELIVERY")
          @RequestParam(required = false)
          List<PerkAppliesTo> appliesTo) {
    return userMembershipQueryManager.getActivePerks(
        userId, appliesTo == null ? Set.of() : Set.copyOf(appliesTo));
  }

  @GetMapping("/api/v1/users/{userId}/memberships/history")
  @Operation(
      summary = "Get paginated membership history",
      description =
          "Returns current and previous subscription records in page order, including"
              + " payment/refund summaries. An existing user with no memberships receives an empty"
              + " page.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Paginated membership history."),
    @ApiResponse(
        responseCode = "400",
        description = "page must be at least 0 and size must be between 1 and 100."),
    @ApiResponse(responseCode = "404", description = "User does not exist.")
  })
  public PageResponse<MembershipSubscriptionResponse> getHistory(
      @PathVariable java.util.UUID userId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return userMembershipQueryManager.getHistory(userId, PageRequest.of(page, size));
  }
}

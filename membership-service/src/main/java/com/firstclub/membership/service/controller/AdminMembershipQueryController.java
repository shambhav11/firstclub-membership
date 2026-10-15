package com.firstclub.membership.service.controller;

import com.firstclub.membership.core.query.AdminMembershipQueryManager;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.PerkStatus;
import com.firstclub.membership.models.TierStatus;
import com.firstclub.membership.models.common.PageResponse;
import com.firstclub.membership.models.response.admin.MembershipPerkResponse;
import com.firstclub.membership.models.response.admin.MembershipPlanResponse;
import com.firstclub.membership.models.response.admin.MembershipTierCriteriaResponse;
import com.firstclub.membership.models.response.admin.MembershipTierResponse;
import com.firstclub.membership.models.response.admin.MembershipTierSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@Tag(
    name = "Admin membership reads",
    description = "Tier, plan, perk, and criteria configuration reads.")
public class AdminMembershipQueryController {
  private final AdminMembershipQueryManager membershipQueryManager;

  public AdminMembershipQueryController(AdminMembershipQueryManager membershipQueryManager) {
    this.membershipQueryManager = membershipQueryManager;
  }

  @GetMapping("/membership-tiers/{tierId}/criteria")
  @Operation(
      summary = "Get a tier's criteria configuration",
      description =
          "Returns the recursive criteria expression, evaluation window, granted plan, and unique"
              + " priority. The evaluator selects the lowest numeric priority among matching"
              + " criteria.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Criteria configuration."),
    @ApiResponse(
        responseCode = "404",
        description = "Tier or criteria configuration was not found.")
  })
  public MembershipTierCriteriaResponse getTierCriteria(@PathVariable UUID tierId) {
    return membershipQueryManager.getTierCriteria(tierId);
  }

  @GetMapping("/membership-tiers")
  @Operation(
      summary = "List membership tiers",
      description =
          "Returns a paginated administrative tier summary. Optionally filter by ACTIVE or INACTIVE"
              + " status; this endpoint includes enrollment-closed tiers for administration.")
  @ApiResponse(responseCode = "200", description = "Paginated tier summaries.")
  public PageResponse<MembershipTierSummaryResponse> getTiers(
      @RequestParam(required = false) TierStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return membershipQueryManager.getTiers(status, pageable(page, size));
  }

  @GetMapping("/membership-tiers/{tierId}")
  @Operation(
      summary = "Get an administrative tier summary",
      description =
          "Returns tier state and enrollment setting without expanding all plans and perks.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Tier summary."),
    @ApiResponse(responseCode = "404", description = "Tier was not found.")
  })
  public MembershipTierSummaryResponse getTier(@PathVariable UUID tierId) {
    return membershipQueryManager.getTier(tierId);
  }

  @GetMapping("/membership-tiers/{tierId}/summary")
  @Operation(
      summary = "Get a tier with all configured plans and perks",
      description =
          "Returns the full administrative view: tier, all plans, and mapped perks including their"
              + " typed application and display text.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Detailed tier configuration."),
    @ApiResponse(responseCode = "404", description = "Tier was not found.")
  })
  public MembershipTierResponse getDetailedTierSummary(@PathVariable UUID tierId) {
    return membershipQueryManager.getDetailedTierSummary(tierId);
  }

  @GetMapping("/membership-tiers/{tierId}/plans")
  @Operation(
      summary = "List a tier's plans",
      description =
          "Returns all plans for one tier, including inactive or enrollment-closed plans, for"
              + " administrative management.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Paginated plans."),
    @ApiResponse(responseCode = "404", description = "Tier was not found.")
  })
  public PageResponse<MembershipPlanResponse> getTierPlans(
      @PathVariable UUID tierId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return membershipQueryManager.getTierPlans(tierId, pageable(page, size));
  }

  @GetMapping("/membership-tiers/{tierId}/perks")
  @Operation(
      summary = "List a tier's mapped perks",
      description =
          "Returns every mapped perk for administration, including archived configurations when"
              + " they remain mapped.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Paginated mapped perks."),
    @ApiResponse(responseCode = "404", description = "Tier was not found.")
  })
  public PageResponse<MembershipPerkResponse> getTierPerks(
      @PathVariable UUID tierId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return membershipQueryManager.getTierPerks(tierId, pageable(page, size));
  }

  @GetMapping("/membership-perks")
  @Operation(
      summary = "Search reusable perks",
      description =
          "Returns a paginated perk library with mapped tier references. Filter by status or"
              + " appliesTo, search normalized display text, or use `unassignedToTierId` to find"
              + " perks not yet mapped to a specific tier.")
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Paginated reusable perks."),
    @ApiResponse(responseCode = "400", description = "Pagination or enum filter is invalid."),
    @ApiResponse(
        responseCode = "404",
        description = "unassignedToTierId was supplied but the tier was not found.")
  })
  public PageResponse<MembershipPerkResponse> getPerks(
      @RequestParam(required = false) PerkStatus status,
      @RequestParam(required = false) PerkAppliesTo appliesTo,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID unassignedToTierId,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return membershipQueryManager.getPerks(
        status, appliesTo, search, unassignedToTierId, pageable(page, size));
  }

  private Pageable pageable(int page, int size) {
    return PageRequest.of(page, size);
  }
}

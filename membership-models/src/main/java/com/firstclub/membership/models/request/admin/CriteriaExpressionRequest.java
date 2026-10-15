package com.firstclub.membership.models.request.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.firstclub.membership.models.CriteriaOperator;
import com.firstclub.membership.models.CriteriaParameter;
import jakarta.validation.Valid;
import java.util.List;

/**
 * A node is either a group (operator plus children) or a condition (parameter, operator and
 * threshold).
 */
public record CriteriaExpressionRequest(
    CriteriaParameter parameter,
    CriteriaOperator operator,
    JsonNode threshold,
    List<@Valid CriteriaExpressionRequest> children) {
  public CriteriaExpressionRequest {
    children = children == null ? List.of() : List.copyOf(children);
  }
}

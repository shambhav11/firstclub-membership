package com.firstclub.membership.commons;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Boundary owned by the external user service. */
@Component
public class UserServiceInterface {

  private final Map<UUID, Set<String>> cohortsByUser = new ConcurrentHashMap<>();
  private final Set<UUID> criteriaEvaluationCandidates = ConcurrentHashMap.newKeySet();

  // TODO: Add service client

  public UserServiceInterface() {}

  public boolean userExists(UUID userId) {
    if (userId != null) return true;
    return false;
  }

  public String getUserCohort(UUID userId) {
    return getUserCohorts(userId).stream().findFirst().orElse("");
  }

  /**
   * Allows a real user client to expose multi-cohort membership without changing criteria
   * evaluation.
   */
  public Set<String> getUserCohorts(UUID userId) {
    return cohortsByUser.getOrDefault(userId, Set.of());
  }

  /**
   * Returns users whose membership tier should be evaluated. The real user-service client can page
   * all active users or a cohort-maintained candidate set without changing the job.
   */
  public Set<UUID> findUsersForCriteriaEvaluation() {
    return Set.copyOf(criteriaEvaluationCandidates);
  }

  /** Replaced by the eventual user-service client; exposed only for the assignment test API. */
  public void setCohortsForTesting(UUID userId, Set<String> cohorts) {
    cohortsByUser.put(userId, Set.copyOf(cohorts));
    criteriaEvaluationCandidates.add(userId);
  }
}

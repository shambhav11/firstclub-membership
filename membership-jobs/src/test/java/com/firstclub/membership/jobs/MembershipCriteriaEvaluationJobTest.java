package com.firstclub.membership.jobs;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.command.MembershipCriteriaEvaluationManager;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MembershipCriteriaEvaluationJobTest {
  @Test
  void evaluatesEveryCandidateEvenWhenOneEvaluationFails() {
    UserServiceInterface users = mock(UserServiceInterface.class);
    MembershipCriteriaEvaluationManager evaluator = mock(MembershipCriteriaEvaluationManager.class);
    UUID failingUser = UUID.randomUUID();
    UUID succeedingUser = UUID.randomUUID();
    when(users.findUsersForCriteriaEvaluation()).thenReturn(Set.of(failingUser, succeedingUser));
    doThrow(new IllegalStateException("upstream unavailable"))
        .when(evaluator)
        .evaluate(failingUser, com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);

    new MembershipCriteriaEvaluationJob(users, evaluator).evaluateMonthly();

    verify(evaluator)
        .evaluate(failingUser, com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
    verify(evaluator)
        .evaluate(succeedingUser, com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
  }
}

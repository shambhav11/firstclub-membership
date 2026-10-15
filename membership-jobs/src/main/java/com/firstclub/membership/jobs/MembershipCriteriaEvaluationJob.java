package com.firstclub.membership.jobs;

import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.core.command.MembershipCriteriaEvaluationManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the tier evaluator monthly; each user retains the evaluator's Redis checkout lock. */
@Component
public class MembershipCriteriaEvaluationJob {
  private static final Log log = LogFactory.getLog(MembershipCriteriaEvaluationJob.class);
  private final UserServiceInterface userServiceInterface;
  private final MembershipCriteriaEvaluationManager membershipCriteriaEvaluationManager;

  public MembershipCriteriaEvaluationJob(
      UserServiceInterface userServiceInterface,
      MembershipCriteriaEvaluationManager membershipCriteriaEvaluationManager) {
    this.userServiceInterface = userServiceInterface;
    this.membershipCriteriaEvaluationManager = membershipCriteriaEvaluationManager;
  }

  /**
   * First day of every month at 02:00 Asia/Kolkata by default; configurable for operations/tests.
   */
  @Scheduled(cron = "${membership.jobs.criteria-cron:0 0 2 1 * *}", zone = "Asia/Kolkata")
  public void evaluateMonthly() {
    for (var userId : userServiceInterface.findUsersForCriteriaEvaluation()) {
      try {
        membershipCriteriaEvaluationManager.evaluate(
            userId, com.firstclub.membership.data.entity.AuditableEntity.SYSTEM_ACTOR);
      } catch (RuntimeException exception) {
        // One unavailable user or upstream fact must not prevent all remaining users being
        // evaluated.
        log.warn("Criteria evaluation failed for user " + userId, exception);
      }
    }
  }
}

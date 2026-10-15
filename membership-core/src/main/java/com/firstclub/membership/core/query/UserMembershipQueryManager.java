package com.firstclub.membership.core.query;

import com.firstclub.membership.commons.ResourceNotFoundException;
import com.firstclub.membership.commons.UserServiceInterface;
import com.firstclub.membership.data.entity.MembershipSubscriptionEntity;
import com.firstclub.membership.data.mappers.PageResponseMapper;
import com.firstclub.membership.models.PerkAppliesTo;
import com.firstclub.membership.models.common.PageResponse;
import com.firstclub.membership.models.response.user.MembershipSubscriptionResponse;
import com.firstclub.membership.models.response.user.UserMembershipPerkResponse;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserMembershipQueryManager {
  private final UserServiceInterface userServiceInterface;
  private final MembershipSubscriptionFetchService membershipSubscriptionFetchService;
  private final UserMembershipResponseHelper userMembershipResponseHelper;
  private final PageResponseMapper pageMapper;
  private final UserPerksFetchHelper userPerksFetchHelper;

  public UserMembershipQueryManager(
      UserServiceInterface userServiceInterface,
      MembershipSubscriptionFetchService membershipSubscriptionFetchService,
      UserMembershipResponseHelper userMembershipResponseHelper,
      PageResponseMapper pageResponseMapper,
      UserPerksFetchHelper userPerksFetchHelper) {
    this.userServiceInterface = userServiceInterface;
    this.membershipSubscriptionFetchService = membershipSubscriptionFetchService;
    this.userMembershipResponseHelper = userMembershipResponseHelper;
    this.pageMapper = pageResponseMapper;
    this.userPerksFetchHelper = userPerksFetchHelper;
  }

  public MembershipSubscriptionResponse getCurrent(UUID userId) {
    requireUser(userId);
    return toResponse(
        membershipSubscriptionFetchService
            .findActiveByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Active membership", userId)));
  }

  public PageResponse<MembershipSubscriptionResponse> getHistory(UUID userId, Pageable pageable) {
    requireUser(userId);
    var history = membershipSubscriptionFetchService.getHistory(userId, pageable);
    var responses = userMembershipResponseHelper.toResponses(history.getContent());
    return new PageResponse<>(
        responses,
        history.getNumber(),
        history.getSize(),
        history.getTotalElements(),
        history.getTotalPages(),
        history.hasNext(),
        history.hasPrevious());
  }

  /** Returns active perks for the user's active membership; users without one receive no perks. */
  public List<UserMembershipPerkResponse> getActivePerks(
      UUID userId, Set<PerkAppliesTo> appliesTo) {
    requireUser(userId);
    return membershipSubscriptionFetchService
        .findActiveByUserId(userId)
        .map(subscription -> userPerksFetchHelper.getActivePerks(subscription, appliesTo))
        .orElseGet(List::of);
  }

  public MembershipSubscriptionResponse toResponse(MembershipSubscriptionEntity subscription) {
    return userMembershipResponseHelper.toResponse(subscription);
  }

  private void requireUser(UUID userId) {
    if (!userServiceInterface.userExists(userId))
      throw new ResourceNotFoundException("User", userId);
  }
}

package com.firstclub.membership.models.request;

/** Identifies the caller responsible for a state-changing request. */
public interface AuditedRequest {
  String requestedBy();
}

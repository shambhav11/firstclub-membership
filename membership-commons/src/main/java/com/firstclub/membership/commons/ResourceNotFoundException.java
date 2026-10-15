package com.firstclub.membership.commons;

public class ResourceNotFoundException extends RuntimeException {
  public ResourceNotFoundException(String resource, Object id) {
    super(resource + " not found: " + id);
  }
}

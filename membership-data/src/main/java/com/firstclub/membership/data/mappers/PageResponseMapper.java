package com.firstclub.membership.data.mappers;

import com.firstclub.membership.models.common.PageResponse;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class PageResponseMapper {
  public <T, R> PageResponse<R> toResponse(Page<T> page, Function<T, R> itemMapper) {
    return new PageResponse<>(
        page.getContent().stream().map(itemMapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.hasNext(),
        page.hasPrevious());
  }
}

package com.nyxn.ecommerce.infrastructure.adapter.in.dto;

import com.nyxn.ecommerce.domain.model.PagedResult;

import java.util.List;

public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> fromPagedResult(PagedResult<T> result) {
        return new PagedResponse<>(
            result.content(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages(),
            result.first(),
            result.last()
        );
    }
}

package com.nyxn.ecommerce.domain.model;

import java.util.List;
import java.util.function.Function;

public record PagedResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);
        return new PagedResult<>(
            content,
            page,
            size,
            totalElements,
            totalPages,
            page == 0,
            page >= totalPages - 1
        );
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        List<R> mappedContent = content.stream().map(mapper).toList();
        return new PagedResult<>(
            mappedContent,
            this.page,
            this.size,
            this.totalElements,
            this.totalPages,
            this.first,
            this.last
        );
    }
}

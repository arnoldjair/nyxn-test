package com.nyxn.ecommerce.domain.model;

public record PaginationParams(
    int page,
    int size,
    String sortBy,
    String sortDirection
) {
    public static PaginationParams of(int page, int size) {
        return new PaginationParams(page, size, "createdAt", "DESC");
    }
}

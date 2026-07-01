package com.nyxn.ecommerce.infrastructure.adapter.in.dto;

import com.nyxn.ecommerce.domain.model.Product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category,
    LocalDateTime createdAt
) {
    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCategory(),
            product.getCreatedAt()
        );
    }
}

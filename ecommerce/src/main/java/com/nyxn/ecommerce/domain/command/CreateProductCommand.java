package com.nyxn.ecommerce.domain.command;

import java.math.BigDecimal;

public record CreateProductCommand(
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category
) {}

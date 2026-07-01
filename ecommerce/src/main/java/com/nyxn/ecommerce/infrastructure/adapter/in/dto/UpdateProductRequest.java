package com.nyxn.ecommerce.infrastructure.adapter.in.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductRequest(
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @Min(value = 0, message = "Stock cannot be negative")
    Integer stock,

    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category
) {}

package com.nyxn.ecommerce.domain.usecase;

import com.nyxn.ecommerce.domain.command.CreateProductCommand;
import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;

import java.time.LocalDateTime;

public class CreateProductUseCase {
    private final ProductRepositoryPort repository;

    public CreateProductUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public Product execute(CreateProductCommand command) {
        Product product = Product.builder()
            .name(command.name())
            .description(command.description())
            .price(command.price())
            .stock(command.stock())
            .category(command.category())
            .createdAt(LocalDateTime.now())
            .build();
        return repository.save(product);
    }
}

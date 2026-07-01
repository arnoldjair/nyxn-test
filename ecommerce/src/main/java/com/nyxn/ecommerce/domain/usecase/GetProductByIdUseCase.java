package com.nyxn.ecommerce.domain.usecase;

import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;
import com.nyxn.ecommerce.infrastructure.exception.ResourceNotFoundException;

import java.util.Optional;

public class GetProductByIdUseCase {
    private final ProductRepositoryPort repository;

    public GetProductByIdUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public Product execute(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }
}

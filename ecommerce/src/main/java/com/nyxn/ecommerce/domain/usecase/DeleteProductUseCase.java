package com.nyxn.ecommerce.domain.usecase;

import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;
import com.nyxn.ecommerce.infrastructure.exception.ResourceNotFoundException;

public class DeleteProductUseCase {
    private final ProductRepositoryPort repository;

    public DeleteProductUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public void execute(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        repository.deleteById(id);
    }
}

package com.nyxn.ecommerce.domain.usecase;

import com.nyxn.ecommerce.domain.command.UpdateProductCommand;
import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;
import com.nyxn.ecommerce.infrastructure.exception.ResourceNotFoundException;

public class UpdateProductUseCase {
    private final ProductRepositoryPort repository;

    public UpdateProductUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public Product execute(UpdateProductCommand command) {
        Product product = repository.findById(command.id())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + command.id()));

        if (command.name() != null) {
            product.setName(command.name());
        }
        if (command.description() != null) {
            product.setDescription(command.description());
        }
        if (command.price() != null) {
            product.setPrice(command.price());
        }
        if (command.stock() != null) {
            product.setStock(command.stock());
        }
        if (command.category() != null) {
            product.setCategory(command.category());
        }

        return repository.save(product);
    }
}

package com.nyxn.ecommerce.domain.usecase;

import com.nyxn.ecommerce.domain.model.PagedResult;
import com.nyxn.ecommerce.domain.model.PaginationParams;
import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;

public class GetAllProductsUseCase {
    private final ProductRepositoryPort repository;

    public GetAllProductsUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public PagedResult<Product> execute(PaginationParams params) {
        return repository.findAll(params);
    }
}

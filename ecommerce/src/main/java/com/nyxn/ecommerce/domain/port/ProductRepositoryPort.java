package com.nyxn.ecommerce.domain.port;

import com.nyxn.ecommerce.domain.model.PagedResult;
import com.nyxn.ecommerce.domain.model.PaginationParams;
import com.nyxn.ecommerce.domain.model.Product;

import java.util.Optional;

public interface ProductRepositoryPort {
    PagedResult<Product> findAll(PaginationParams params);
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);
    boolean existsById(Long id);
}

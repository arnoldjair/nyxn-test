package com.nyxn.ecommerce.infrastructure.adapter.out.persistence;

import com.nyxn.ecommerce.domain.model.PagedResult;
import com.nyxn.ecommerce.domain.model.PaginationParams;
import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;
import com.nyxn.ecommerce.infrastructure.adapter.out.jpa.ProductEntity;
import com.nyxn.ecommerce.infrastructure.adapter.out.jpa.ProductJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResult<Product> findAll(PaginationParams params) {
        Sort sort = Sort.by(
            params.sortDirection().equalsIgnoreCase("ASC") ? Sort.Direction.ASC : Sort.Direction.DESC,
            params.sortBy()
        );
        Pageable pageable = PageRequest.of(params.page(), params.size(), sort);

        Page<ProductEntity> page = jpaRepository.findAll(pageable);

        List<Product> products = page.getContent().stream()
            .map(this::toDomain)
            .toList();

        return PagedResult.of(products, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductEntity entity = toEntity(product);
        ProductEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return jpaRepository.existsById(id);
    }

    private Product toDomain(ProductEntity entity) {
        return Product.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .price(entity.getPrice())
            .stock(entity.getStock())
            .category(entity.getCategory())
            .createdAt(entity.getCreatedAt())
            .build();
    }

    private ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setName(product.getName());
        entity.setDescription(product.getDescription());
        entity.setPrice(product.getPrice());
        entity.setStock(product.getStock());
        entity.setCategory(product.getCategory());
        entity.setCreatedAt(product.getCreatedAt());
        return entity;
    }
}

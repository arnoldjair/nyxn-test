package com.nyxn.ecommerce.infrastructure.config;

import com.nyxn.ecommerce.domain.port.ProductRepositoryPort;
import com.nyxn.ecommerce.domain.usecase.*;
import com.nyxn.ecommerce.infrastructure.adapter.out.persistence.ProductRepositoryAdapter;
import com.nyxn.ecommerce.infrastructure.adapter.out.jpa.ProductJpaRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProductRepositoryPort productRepositoryPort(ProductJpaRepository jpaRepository) {
        return new ProductRepositoryAdapter(jpaRepository);
    }

    @Bean
    public CreateProductUseCase createProductUseCase(ProductRepositoryPort repositoryPort) {
        return new CreateProductUseCase(repositoryPort);
    }

    @Bean
    public GetAllProductsUseCase getAllProductsUseCase(ProductRepositoryPort repositoryPort) {
        return new GetAllProductsUseCase(repositoryPort);
    }

    @Bean
    public GetProductByIdUseCase getProductByIdUseCase(ProductRepositoryPort repositoryPort) {
        return new GetProductByIdUseCase(repositoryPort);
    }

    @Bean
    public UpdateProductUseCase updateProductUseCase(ProductRepositoryPort repositoryPort) {
        return new UpdateProductUseCase(repositoryPort);
    }

    @Bean
    public DeleteProductUseCase deleteProductUseCase(ProductRepositoryPort repositoryPort) {
        return new DeleteProductUseCase(repositoryPort);
    }
}

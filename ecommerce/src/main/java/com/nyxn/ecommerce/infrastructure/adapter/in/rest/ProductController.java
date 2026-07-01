package com.nyxn.ecommerce.infrastructure.adapter.in.rest;

import com.nyxn.ecommerce.domain.command.CreateProductCommand;
import com.nyxn.ecommerce.domain.command.UpdateProductCommand;
import com.nyxn.ecommerce.domain.model.PagedResult;
import com.nyxn.ecommerce.domain.model.PaginationParams;
import com.nyxn.ecommerce.domain.model.Product;
import com.nyxn.ecommerce.domain.usecase.*;
import com.nyxn.ecommerce.infrastructure.adapter.in.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management API")
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetAllProductsUseCase getAllProductsUseCase;
    private final GetProductByIdUseCase getProductByIdUseCase;
    private final UpdateProductUseCase updateProductUseCase;
    private final DeleteProductUseCase deleteProductUseCase;

    @GetMapping
    @Operation(summary = "List all products", description = "Returns a paginated list of products")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    })
    public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        PaginationParams params = new PaginationParams(page, size, sortBy, sortDir);
        PagedResult<Product> pagedResult = getAllProductsUseCase.execute(params);
        PagedResponse<ProductResponse> response = PagedResponse.fromPagedResult(
            pagedResult.map(ProductResponse::fromDomain)
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        Product product = getProductByIdUseCase.execute(id);
        return ResponseEntity.ok(ProductResponse.fromDomain(product));
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Adds a product to the catalog")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        CreateProductCommand command = new CreateProductCommand(
            request.name(),
            request.description(),
            request.price(),
            request.stock(),
            request.category()
        );
        Product product = createProductUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ProductResponse.fromDomain(product));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        UpdateProductCommand command = new UpdateProductCommand(
            id,
            request.name(),
            request.description(),
            request.price(),
            request.stock(),
            request.category()
        );
        Product product = updateProductUseCase.execute(command);
        return ResponseEntity.ok(ProductResponse.fromDomain(product));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        deleteProductUseCase.execute(id);
        return ResponseEntity.noContent().build();
    }
}

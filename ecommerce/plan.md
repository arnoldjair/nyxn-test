# Plan: API RESTful con Clean Architecture — NYXN Ecommerce

## Objetivo
Implementar API RESTful para gestión de catálogo de productos con estructura Clean Architecture / Hexagonal, cumpliendo estrictamente con los requerimientos de nivel Senior (Paginación nativa, Swagger, validaciones y buenas prácticas de BD).

---

## Estructura de Paquetes

```
src/main/java/com/nyxn/ecommerce/
│
├── domain/                              # 🟢 CORE: 0% Spring, 100% Java Puro
│   ├── model/
│   │   ├── Product.java                 # Lombok: @Data @Builder @AllArgsConstructor @NoArgsConstructor
│   │   ├── PaginationParams.java        # record: page, size, sortBy, sortDirection
│   │   └── PagedResult.java             # record: content, page, size, totalElements, totalPages, first, last
│   ├── command/
│   │   ├── CreateProductCommand.java    # record normal
│   │   └── UpdateProductCommand.java    # record normal (campos nullables para actualización parcial)
│   ├── port/
│   │   └── ProductRepositoryPort.java   # Interface pura Java
│   └── usecase/
│       ├── CreateProductUseCase.java     # Lógica de creación (asigna createdAt)
│       ├── GetAllProductsUseCase.java
│       ├── GetProductByIdUseCase.java
│       ├── UpdateProductUseCase.java     # Lógica de actualización parcial (ignora nulos)
│       └── DeleteProductUseCase.java
│
├── infrastructure/                      # 🔵 INFRAESTRUCTURA: Spring, JPA, Web
│   ├── adapter/
│   │   ├── in/
│   │   │   ├── rest/
│   │   │   │   └── ProductController.java  # Usa @Operation, @ApiResponse, recibe Pageable de Spring
│   │   │   └── dto/
│   │   │       ├── CreateProductRequest.java
│   │   │       ├── UpdateProductRequest.java
│   │   │       ├── ProductResponse.java
│   │   │       └── PagedResponse.java
│   │   └── out/
│   │       ├── jpa/
│   │       │   ├── ProductEntity.java      # @Entity, @EntityListeners(AuditingEntityListener.class)
│   │       │   └── ProductJpaRepository.java
│   │       └── persistence/
│   │           └── ProductRepositoryAdapter.java
│   │
│   ├── config/
│   │   ├── OpenApiConfig.java           # Configuración Swagger
│   │   └── UseCaseConfig.java
│   │
│   └── exception/
│       ├── GlobalExceptionHandler.java   # @RestControllerAdvice (400, 404, 500)
│       ├── ResourceNotFoundException.java
│       └── ErrorResponse.java            # Estructura estándar para errores
│
└── EcommerceApplication.java
```

---

## Modelo de Datos: Product

| Campo | Tipo | Validaciones en DTO de Entrada |
|-------|------|-------------------------------|
| `id` | Long | — |
| `name` | String | @NotBlank, @Size(min=3, max=100) |
| `description` | String | @Size(max=500) |
| `price` | BigDecimal | @NotNull, @DecimalMin(value="0.01") |
| `stock` | Integer | @NotNull, @Min(0) |
| `category` | String | @NotBlank, @Size(max=50) |
| `createdAt` | LocalDateTime | Generado automáticamente con @CreatedDate |

---

## Endpoints

| Método | Endpoint | Use Case | Request Body | Response |
|--------|----------|----------|--------------|----------|
| GET | `/products` | GetAllProductsUseCase | Query params: page, size, sortBy, sortDir | PagedResponse |
| GET | `/products/{id}` | GetProductByIdUseCase | Path: id | ProductResponse |
| POST | `/products` | CreateProductUseCase | CreateProductRequest | ProductResponse (201) |
| PUT | `/products/{id}` | UpdateProductUseCase | Path: id, Body: UpdateProductRequest | ProductResponse |
| DELETE | `/products/{id}` | DeleteProductUseCase | Path: id | 204 No Content |

---

## Endpoints — Query Params para GET /products

| Param | Tipo | Default | Descripción |
|-------|------|---------|-------------|
| `page` | int | 0 | Página (0-indexed) |
| `size` | int | 10 | Elementos por página |
| `sortBy` | String | "createdAt" | Campo de ordenamiento |
| `sortDir` | String | "DESC" | Dirección: ASC o DESC |

---

## Componentes del Dominio (100% Java Puro)

### domain/model/Product.java
```java
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String category;
    private LocalDateTime createdAt;
}
```

### domain/model/PaginationParams.java
```java
public record PaginationParams(
    int page,
    int size,
    String sortBy,
    String sortDirection
)
```

### domain/model/PagedResult.java
```java
public record PagedResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
)
```

### domain/command/CreateProductCommand.java
```java
public record CreateProductCommand(
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category
)
```

### domain/command/UpdateProductCommand.java
```java
public record UpdateProductCommand(
    Long id,
    String name,           // null = no actualizar
    String description,    // null = no actualizar
    BigDecimal price,      // null = no actualizar
    Integer stock,         // null = no actualizar
    String category        // null = no actualizar
)
```

### domain/port/ProductRepositoryPort.java
```java
public interface ProductRepositoryPort {
    PagedResult<Product> findAll(PaginationParams params);
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

### domain/usecase/*UseCase.java
- Todos con método único `execute()`
- Constructor recibe `ProductRepositoryPort`
- Sin anotaciones Spring
- UpdateProductUseCase evalúa `if (campo != null)` para actualización parcial

---

## Componentes de Infrastructure

### adapter/out/jpa/ProductEntity.java
```java
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 50)
    private String category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
```

### adapter/out/jpa/ProductJpaRepository.java
```java
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
}
```

### adapter/out/persistence/ProductRepositoryAdapter.java
```java
@Repository
public class ProductRepositoryAdapter implements ProductRepositoryPort {

    private final ProductJpaRepository jpaRepository;

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
            .map(ProductMapper::toDomain)
            .toList();

        return PagedResult.of(products, page.getNumber(), page.getSize(),
                              page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id).map(ProductMapper::toDomain);
    }

    @Override
    @Transactional
    public Product save(Product product) {
        ProductEntity entity = ProductMapper.toEntity(product);
        ProductEntity saved = jpaRepository.save(entity);
        return ProductMapper.toDomain(saved);
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
}
```

### adapter/in/dto/CreateProductRequest.java
```java
public record CreateProductRequest(
    @NotBlank(message = "Name is required")
    @Size(min = 3, max = 100, message = "Name must be between 3 and 100 characters")
    String name,

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    String description,

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    BigDecimal price,

    @NotNull(message = "Stock is required")
    @Min(value = 0, message = "Stock cannot be negative")
    Integer stock,

    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category cannot exceed 50 characters")
    String category
)
```

### adapter/in/dto/UpdateProductRequest.java
```java
public record UpdateProductRequest(
    @Size(min = 3, max = 100) String name,
    @Size(max = 500) String description,
    @DecimalMin(value = "0.01") BigDecimal price,
    @Min(value = 0) Integer stock,
    @Size(max = 50) String category
)
```
**Nota:** Campos nullables. Si llega null, no se actualiza ese campo.

### adapter/in/dto/ProductResponse.java
```java
public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category,
    LocalDateTime createdAt
) {
    public static ProductResponse fromDomain(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCategory(),
            product.getCreatedAt()
        );
    }
}
```

### adapter/in/dto/PagedResponse.java
```java
public record PagedResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
    public static <T> PagedResponse<T> fromPagedResult(PagedResult<T> result) {
        return new PagedResponse<>(
            result.content(),
            result.page(),
            result.size(),
            result.totalElements(),
            result.totalPages(),
            result.first(),
            result.last()
        );
    }
}
```

### adapter/in/rest/ProductController.java
```java
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
```

### infrastructure/config/OpenApiConfig.java
```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NYXN Ecommerce Product API")
                .version("1.0")
                .description("RESTful API for e-commerce product catalog management")
                .contact(new Contact()
                    .name("NYXN Team")
                    .email("dev@nyxn.com")
                )
            );
    }
}
```

### infrastructure/config/UseCaseConfig.java
```java
@Configuration
public class UseCaseConfig {

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
```

### infrastructure/exception/ResourceNotFoundException.java
```java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
```

### infrastructure/exception/ErrorResponse.java
```java
public record ErrorResponse(
    int status,
    String message,
    LocalDateTime timestamp,
    Map<String, String> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now(), null);
    }

    public ErrorResponse(int status, String message, Map<String, String> errors) {
        this(status, message, LocalDateTime.now(), errors);
    }
}
```

### infrastructure/exception/GlobalExceptionHandler.java
```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });

        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Internal server error: " + ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## Migración Flyway

### src/main/resources/db/migration/V1__Create_products_table.sql
```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    stock INTEGER NOT NULL,
    category VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_created_at ON products(created_at);
```

---

## application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/nyxn_db
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

---

## Flujo de Dependencias

```
HTTP Request
    ↓
ProductController (adapter/in/rest)
    ↓ (mapeo Request → Command)
CreateProductUseCase.execute(command) (domain/usecase)
    ↓ (inyección por constructor)
ProductRepositoryPort (domain/port)
    ↓ (implementado por)
ProductRepositoryAdapter (adapter/out/persistence)
    ↓ (usa)
ProductJpaRepository (adapter/out/jpa)
    ↓
PostgreSQL + Flyway
```

---

## Estrategia de Paginación Nativa (Requisito Senior)

El Controller recibe `Pageable` de Spring para cumplir con "Paginación robusta implementada nativamente con Pageable":

```java
@GetMapping
public ResponseEntity<PagedResponse<ProductResponse>> getAllProducts(
    Pageable pageable  // Spring inyecta automáticamente
) {
    PaginationParams params = new PaginationParams(
        pageable.getPageNumber(),
        pageable.getPageSize(),
        pageable.getSort().getOrderFor("createdAt").getProperty(),
        pageable.getSort().getOrderFor("createdAt").getDirection().name()
    );
    // ... resto del flujo
}
```

**Traducción limpia:** Cumple el requisito de Spring sin ensuciar la Clean Architecture.

---

## Estrategia de Actualización Parcial

**UpdateProductCommand** usa campos nullables (NO Optional):

```java
// UpdateProductUseCase.java
public Product execute(UpdateProductCommand command) {
    Product product = repository.findById(command.id())
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

    if (command.name() != null) {
        product.setName(command.name());
    }
    if (command.price() != null) {
        product.setPrice(command.price());
    }
    if (command.stock() != null) {
        product.setStock(command.stock());
    }
    // ... resto de campos

    return repository.save(product);
}
```

---

## Anotaciones por Capa

| Capa | Anotaciones Permitidas |
|------|------------------------|
| domain/model/ | Solo Lombok (@Data, @Builder, etc.) |
| domain/command/ | Ninguna (record puro) |
| domain/port/ | Ninguna (interface pura) |
| domain/usecase/ | Ninguna |
| adapter/in/ | @RestController, @RequestMapping, @Valid, @Tag, @Operation, @ApiResponse |
| adapter/out/jpa/ | @Entity, @Table, @Id, @GeneratedValue, @Column, @EntityListeners, @CreatedDate |
| adapter/out/persistence/ | @Repository, @Transactional |
| infrastructure/config/ | @Configuration, @Bean |
| infrastructure/exception/ | @RestControllerAdvice, @ExceptionHandler |

---

## Excepciones y Códigos HTTP

| Escenario | Exception | HTTP Status |
|-----------|-----------|-------------|
| Producto no existe | ResourceNotFoundException | 404 |
| Validación falla | MethodArgumentNotValidException (handle) | 400 |
| Error interno | Exception (handle) | 500 |

---

## Archivos a Crear (26 total)

| # | Ubicación | Archivo |
|---|-----------|---------|
| 1 | domain/model/ | Product.java |
| 2 | domain/model/ | PaginationParams.java |
| 3 | domain/model/ | PagedResult.java |
| 4 | domain/command/ | CreateProductCommand.java |
| 5 | domain/command/ | UpdateProductCommand.java |
| 6 | domain/port/ | ProductRepositoryPort.java |
| 7 | domain/usecase/ | CreateProductUseCase.java |
| 8 | domain/usecase/ | GetAllProductsUseCase.java |
| 9 | domain/usecase/ | GetProductByIdUseCase.java |
| 10 | domain/usecase/ | UpdateProductUseCase.java |
| 11 | domain/usecase/ | DeleteProductUseCase.java |
| 12 | adapter/out/jpa/ | ProductEntity.java |
| 13 | adapter/out/jpa/ | ProductJpaRepository.java |
| 14 | adapter/out/persistence/ | ProductRepositoryAdapter.java |
| 15 | adapter/in/dto/ | CreateProductRequest.java |
| 16 | adapter/in/dto/ | UpdateProductRequest.java |
| 17 | adapter/in/dto/ | ProductResponse.java |
| 18 | adapter/in/dto/ | PagedResponse.java |
| 19 | adapter/in/rest/ | ProductController.java |
| 20 | infrastructure/config/ | OpenApiConfig.java |
| 21 | infrastructure/config/ | UseCaseConfig.java |
| 22 | infrastructure/exception/ | GlobalExceptionHandler.java |
| 23 | infrastructure/exception/ | ResourceNotFoundException.java |
| 24 | infrastructure/exception/ | ErrorResponse.java |
| 25 | resources/db/migration/ | V1__Create_products_table.sql |
| 26 | resources/ | application.yml |

---

## Dependencias Requeridas (pom.xml)

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-database-postgresql</artifactId>
    </dependency>

    <!-- OpenAPI / Swagger -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>2.3.0</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- JPA Auditing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
</dependencies>
```

---

## Orden de Implementación Sugerido

1. **Configuración Inicial:** Dependencias (pom.xml), Flyway script
2. **Dominio (Core):** Modelos (Product, PaginationParams, PagedResult), Commands, Puerto
3. **Casos de Uso:** Implementar lógica de los 5 casos de uso
4. **Persistencia (Out):** Entidad JPA, Spring Data Repository, Adapter
5. **Controlador Web (In):** DTOs de Request/Response, ProductController con Swagger
6. **Manejo de Errores:** GlobalExceptionHandler, ErrorResponse
7. **Configuración:** UseCaseConfig para enlazar todo
8. **application.yml:** Configuración de BD y JPA

---

## Estrategia de Testing (Requisito Senior)

### Dominio: Pruebas Unitarias
- JUnit 5 + Mockito para los UseCases
- Validar lógica de creación y actualización parcial

### Infraestructura: Pruebas de Integración
- @DataJpaTest + Testcontainers (PostgreSQL)
- ProductRepositoryAdapter guarda y pagina correctamente en BD real

### Controladores: Pruebas Web
- @WebMvcTest para validar códigos HTTP (201, 400, 404)
- GlobalExceptionHandler captura MethodArgumentNotValidException

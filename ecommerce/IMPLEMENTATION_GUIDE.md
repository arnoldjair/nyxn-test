# NYXN Ecommerce

## 1. Dominio (Domain)

### 1.1 Modelos
```java
// Product.java
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

// PaginationParams.java
public record PaginationParams(
    int page,
    int size,
    String sortBy,
    String sortDirection
)

// PagedResult.java
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

### 1.2 Commands
```java
// CreateProductCommand.java
public record CreateProductCommand(
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category
)

// UpdateProductCommand.java
public record UpdateProductCommand(
    Long id,
    String name,
    String description,
    BigDecimal price,
    Integer stock,
    String category
)
```

### 1.3 Puerto (Port)
```java
// ProductRepositoryPort.java
public interface ProductRepositoryPort {
    PagedResult<Product> findAll(PaginationParams params);
    Optional<Product> findById(Long id);
    Product save(Product product);
    void deleteById(Long id);
    boolean existsById(Long id);
}
```

### 1.4 Casos de Uso (UseCases)
```java
// CreateProductUseCase.java
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

// GetAllProductsUseCase.java
public class GetAllProductsUseCase {
    private final ProductRepositoryPort repository;

    public GetAllProductsUseCase(ProductRepositoryPort repository) {
        this.repository = repository;
    }

    public PagedResult<Product> execute(PaginationParams params) {
        return repository.findAll(params);
    }
}

// GetProductByIdUseCase.java
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

// UpdateProductUseCase.java
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

// DeleteProductUseCase.java
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
```

## 2. Infrastructure - Out (Persistencia)

### 2.1 JPA Entity
```java
// ProductEntity.java
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

// ProductJpaRepository.java
@Repository
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
}
```

### 2.2 Adapter
```java
// ProductRepositoryAdapter.java
@Repository
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

// ProductMapper.java
public class ProductMapper {
    public static Product toDomain(ProductEntity entity) {
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

    public static ProductEntity toEntity(Product product) {
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
```

## 3. Infrastructure - In (API)

### 3.1 DTOs
```java
// CreateProductRequest.java
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

// UpdateProductRequest.java
public record UpdateProductRequest(
    @Size(min = 3, max = 100) String name,
    @Size(max = 500) String description,
    @DecimalMin(value = "0.01") BigDecimal price,
    @Min(value = 0) Integer stock,
    @Size(max = 50) String category
)

// ProductResponse.java
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

// PagedResponse.java
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

### 3.2 Controller
```java
// ProductController.java
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

## 4. Infrastructure - Exception Handling

```java
// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// ErrorResponse.java
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

// GlobalExceptionHandler.java
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

## 5. Infrastructure - Config

```java
// OpenApiConfig.java
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

// UseCaseConfig.java
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

// JpaConfig.java (for Auditing)
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
```

## 6. Resources

### application.yml
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

### V1__Create_products_table.sql
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

## 7. Main Application

```java
// EcommerceApplication.java
@SpringBootApplication
public class EcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EcommerceApplication.class, args);
    }
}
```

## 8. pom.xml dependencies

```xml
<!-- Flyway -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

## 9. Testing Strategy

### Unit Tests (Domain)
```java
// CreateProductUseCaseTest.java
class CreateProductUseCaseTest {
    private ProductRepositoryPort mockRepository;
    private CreateProductUseCase useCase;

    @BeforeEach
    void setUp() {
        mockRepository = mock(ProductRepositoryPort.class);
        useCase = new CreateProductUseCase(mockRepository);
    }

    @Test
    void shouldCreateProduct() {
        CreateProductCommand command = new CreateProductCommand(
            "Test Product", "Description", new BigDecimal("99.99"), 10, "Electronics"
        );
        when(mockRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product result = useCase.execute(command);

        assertNotNull(result);
        assertEquals("Test Product", result.getName());
        verify(mockRepository).save(any(Product.class));
    }
}
```

### Integration Tests (Infrastructure)
```java
// ProductRepositoryAdapterTest.java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryAdapterTest {
    @Autowired
    private ProductJpaRepository jpaRepository;

    private ProductRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductRepositoryAdapter(jpaRepository);
    }

    @Test
    void shouldSaveAndFindProduct() {
        Product product = Product.builder()
            .name("Test")
            .price(new BigDecimal("10.00"))
            .stock(5)
            .category("Test")
            .build();

        Product saved = adapter.save(product);
        Optional<Product> found = adapter.findById(saved.getId());

        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getName());
    }
}
```

### Controller Tests
```java
// ProductControllerTest.java
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateProductUseCase createProductUseCase;
    @MockBean
    private GetAllProductsUseCase getAllProductsUseCase;
    // ... other mocks

    @Test
    void shouldReturn201WhenCreatingProduct() throws Exception {
        when(createProductUseCase.execute(any())).thenReturn(mockProduct());

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\",\"price\":10.00,\"stock\":5,\"category\":\"Test\"}"))
            .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn400WhenValidationFails() throws Exception {
        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"price\":-1}"))
            .andExpect(status().isBadRequest());
    }
}
```

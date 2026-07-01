# Respuestas a Prueba Técnica - Full Stack Developer (NYXN)

## SECCIÓN 1 - JAVA & SPRING BOOT

### Punto 1.2 - Principios SOLID en Código Legacy

**Principios Violados:**

1. **Single Responsibility Principle (SRP):** La clase `OrderService` hace demasiadas cosas: conexión a base de datos, lógica de negocio, peticiones HTTP para pagos y envío de correos electrónicos.

2. **Dependency Inversion Principle (DIP):** El código depende de implementaciones concretas y de bajo nivel (`DriverManager`, `HttpURLConnection`, `Session`) en lugar de depender de abstracciones (interfaces).

3. **Open/Closed Principle (OCP):** Si se añade un nuevo método de pago o se cambia el proveedor de correos, se debe modificar y recompilar esta clase, violando el principio de estar abierta a la extensión pero cerrada a la modificación.

4. **Interface Segregation Principle (ISP) / Liskov Substitution Principle (LSP):** Al carecer de interfaces, es imposible sustituir componentes (por ejemplo, para realizar pruebas unitarias con mocks).

**Refactorización (Clean Code):**

```java
@Service
@RequiredArgsConstructor
public class OrderService {
    
    // Inyección de dependencias mediante abstracciones (Puertos/Interfaces)
    private final OrderRepository orderRepository;
    private final PaymentPort paymentPort;
    private final NotificationPort notificationPort;
    private final InventoryPort inventoryPort;

    @Transactional
    public void processOrder(int orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
            
        // Procesar pago (Síncrono, asume que lanza excepción si falla)
        paymentPort.processPayment(order.getTotal());
        
        // Actualizar inventario
        inventoryPort.decreaseStock(orderId);
        
        // Ejecución asíncrona (No-bloqueante) para notificaciones
        CompletableFuture.runAsync(() -> 
            notificationPort.sendConfirmation(order.getCustomerEmail())
        );
    }
}
```

## SECCIÓN 2 - CLEAN ARCHITECTURE & ARQUITECTURA HEXAGONAL

### Diseño e Implementación

**Estructura de Paquetes:**

```text
com.empresa.orders.
├── domain
│   ├── model/ (Order, Product)
│   ├── port/
│   │   ├── in/ (CreateOrderUseCase, ConfirmOrderUseCase) -> Puertos de Entrada
│   │   └── out/ (OrderRepositoryPort, NotificationPort, EventPublisherPort) -> Puertos de Salida
├── application
│   └── service/ (OrderServiceImpl implementa los UseCases)
└── infrastructure
    ├── adapter/
    │   ├── in/
    │   │   └── web/ (OrderController) -> Adaptador Primario
    │   └── out/
    │       ├── persistence/ (PostgreSqlOrderAdapter) -> Adaptador Secundario
    │       ├── messaging/ (GcpPubSubAdapter) -> Adaptador Secundario
    │       └── notification/ (EmailAdapter) -> Adaptador Secundario
```

**Respuestas Argumentadas:**

* **¿Qué reside en el Núcleo (Dominio)?:** Residen las Entidades de negocio, Value Objects, las interfaces (Puertos) y la lógica de negocio pura (Casos de uso). No tiene dependencias de Spring ni de la base de datos.

* **Puerto vs Adaptador:** Un Puerto es el "Qué" (una interfaz en el dominio que define un contrato). Un Adaptador es el "Cómo" (la implementación técnica en la infraestructura, ej. un repositorio JPA o un cliente REST).

* **¿Dónde viven los Casos de Uso?:** Viven en la capa de Aplicación/Dominio. Orquestan el flujo de datos entre las entidades de dominio y los puertos.

* **GCP Pub/Sub como Adaptador Secundario:** Se categoriza como "Secundario" (Driven Adapter) porque es la aplicación quien "lo maneja" o "lo empuja". El dominio dicta cuándo publicar el evento (flujo de salida), a diferencia de un controlador REST (Primario/Driving) que "empuja" a la aplicación a hacer algo.

## SECCIÓN 3 - LÓGICA DE DESARROLLO (CONCURRENCIA)

**Diferencia y Elección:**

* **Bloqueo Optimista (Optimistic Locking):** Usa un campo `@Version`. Asume que las colisiones son raras. Si dos hilos intentan actualizar al mismo tiempo, el segundo fallará y lanzará una excepción.

* **Bloqueo Pesimista (Pessimistic Locking):** Bloquea la fila en la base de datos desde la lectura hasta el final de la transacción (`FOR UPDATE`). Ningún otro hilo puede leer/escribir esa fila hasta que se libere.

* **Elección para Cyber-Day:** Elegiría **Bloqueo Pesimista**. En un evento de alta concurrencia, las colisiones sobre productos populares son garantizadas. El bloqueo optimista generaría demasiadas excepciones `ObjectOptimisticLockingFailureException`, forzando reintentos constantes que saturarían el servidor y degradarían la UX. El pesimista encola las peticiones a nivel de BD, garantizando integridad.

**Implementación (Spring Data JPA):**

```java
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "3000")})
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id")
    Optional<ProductEntity> findByIdForUpdate(@Param("id") Long id);
}
```

**Manejo de HTTP:** Si ocurre un timeout en la adquisición del bloqueo pesimista (o una colisión en el optimista), se debe retornar un **409 Conflict** (o 429 Too Many Requests si es un pico masivo), informando al cliente que su petición no pudo ser procesada y pidiéndole reintentar.

## SECCIÓN 4 - BASES DE DATOS

### 4.1 SQL Avanzado

*Nota: Adaptado para PostgreSQL (estándar ANSI en su mayoría).*

**A) Top 5 clientes:**

```sql
SELECT c.id, c.name, COUNT(o.id) as total_orders, SUM(o.total) as total_spent, AVG(o.total) as average_ticket
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE o.created_at >= CURRENT_DATE - INTERVAL '30 days'
GROUP BY c.id, c.name
ORDER BY total_spent DESC
LIMIT 5;
```

**B) Productos con stock crítico:**

```sql
WITH RecentSales AS (
    SELECT product_id, SUM(quantity) as total_sold
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.id
    WHERE o.created_at >= DATE_TRUNC('month', CURRENT_DATE)
    GROUP BY product_id
)
SELECT p.name, p.category, p.stock, rs.total_sold
FROM products p
JOIN RecentSales rs ON p.id = rs.product_id
WHERE p.stock < 10 AND rs.total_sold > 50;
```

**C) Reporte de ventas (7 días) con ceros:**

```sql
WITH Last7Days AS (
    SELECT generate_series(CURRENT_DATE - INTERVAL '6 days', CURRENT_DATE, '1 day')::date AS report_date
)
SELECT d.report_date, p.category, COALESCE(SUM(oi.quantity * oi.unit_price), 0) AS total_sales
FROM Last7Days d
CROSS JOIN (SELECT DISTINCT category FROM products) p
LEFT JOIN orders o ON DATE(o.created_at) = d.report_date
LEFT JOIN order_items oi ON o.id = oi.order_id
LEFT JOIN products prod ON oi.product_id = prod.id AND prod.category = p.category
GROUP BY d.report_date, p.category
ORDER BY d.report_date DESC, p.category;
```

### 4.2 Redis como Capa de Caché Empresarial

**Configuración:** Usaría un TTL de **60 minutos** en Redis.
**Mitigación de Cache Stampede:** Utilizaría el atributo `sync = true` de Spring Cache. Esto sincroniza el acceso localmente; si la caché expira, solo un hilo (thread) procesa la consulta hacia la BD mientras los demás esperan, evitando el colapso de la base de datos.

```java
@Service
public class ProductCacheService {
    @Cacheable(value = "products", key = "#id", sync = true)
    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow();
    }
}
```

## SECCIÓN 5 - GOOGLE CLOUD PLATFORM & DOCKER

**Servicio de Cómputo:**
Elegiría **Cloud Run**. Al ser Serverless, escala a 0 automáticamente (ahorrando costos cuando no hay tráfico), cobra solo por los milisegundos de uso y elimina la sobrecarga operacional (mantenimiento de clústeres) que exige GKE, haciéndolo ideal para microservicios web (stateless).

**Dockerfile (Multi-Stage No-Root):**

```dockerfile
# Etapa 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Usuario No-Root por seguridad
RUN addgroup -S springgroup && adduser -S springuser -G springgroup
USER springuser:springgroup
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Secretos y CI/CD:**
Centralizaría las llaves en **Google Secret Manager**. Se integra nativamente agregando la dependencia `spring-cloud-gcp-starter-secretmanager` y referenciando el secreto en el `application.yml` (ej. `${sm://projects/my-proj/secrets/db-password}`).
Para **CI/CD**, usaría GitHub Actions: Al hacer push a `main`, el pipeline compila con Maven, construye la imagen Docker, la empuja a **Artifact Registry** y finalmente ejecuta `gcloud run deploy` para actualizar el servicio en producción sin downtime.

## SECCIÓN 6 - NODE.JS & INTELIGENCIA ARTIFICIAL

### Parte A - Node.js (Patrón Strategy)

```typescript
// interface.ts
export interface NotificationStrategy {
    send(userId: string, message: string): Promise<void>;
}

// strategies.ts
export class EmailStrategy implements NotificationStrategy {
    async send(userId: string, message: string) { /* Lógica Email */ }
}
export class SmsStrategy implements NotificationStrategy {
    async send(userId: string, message: string) { /* Lógica SMS */ }
}

// context.ts (Injectable en NestJS)
import { Injectable, BadRequestException } from '@nestjs/common';

@Injectable()
export class NotificationContext {
    private strategies: Map<string, NotificationStrategy> = new Map();

    constructor() {
        this.strategies.set('email', new EmailStrategy());
        this.strategies.set('sms', new SmsStrategy());
    }

    async notify(userId: string, message: string, channel: string) {
        const strategy = this.strategies.get(channel);
        if (!strategy) throw new BadRequestException(`Channel ${channel} not supported`);
        await strategy.send(userId, message);
    }
}
```

### Parte B - IA Generativa (Claude)

**Enfoque:** Utilizaría un esquema de **Tool Use (Function Calling)**.

* **Argumentación:** RAG es útil para recuperar información estática (ej. políticas de devolución), pero Tool Use permite que el LLM traduzca la intención del usuario en una acción transaccional estructurada (JSON) que el sistema puede ejecutar.

* **Tareas delegables:** Cancelaciones de órdenes, actualizaciones de dirección de envío y consulta de estados de guía.

* **Seguridad Bidireccional:** El cliente *nunca* interactúa directamente con la API de Anthropic. El usuario habla con el backend (autenticado por JWT), el backend le pide a Claude qué acción tomar pasándole un "Tool", Claude devuelve el nombre de la función y los parámetros, el backend **valida permisos y ejecuta la lógica**, y finalmente retorna el resultado a Claude para que formule la respuesta conversacional.

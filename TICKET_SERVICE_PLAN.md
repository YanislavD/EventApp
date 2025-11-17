# Ticket Service Microservice - Детайлен План

## 📋 Общ Преглед

**Ticket Service** ще бъде отделно Spring Boot приложение, което управлява билетите за събития. Той ще комуникира с Main приложението чрез REST API и Feign Client.

---

## 🏗️ Архитектура

### Структура на проектите:
```
EventApp/                    (Main Application - порт 8080)
├── src/
└── pom.xml

ticket-service/              (Microservice - порт 8081)
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ticketservice/
│   │   │       ├── TicketServiceApplication.java
│   │   │       ├── controller/
│   │   │       ├── service/
│   │   │       ├── repository/
│   │   │       ├── model/
│   │   │       ├── dto/
│   │   │       ├── exception/
│   │   │       └── config/
│   │   └── resources/
│   │       └── application.properties
│   └── test/
└── pom.xml
```

---

## 📦 Ticket Entity (в Microservice)

### Промени спрямо текущия Ticket:
- **Премахваме** `@OneToOne` връзката с Subscription
- **Добавяме** `subscriptionId` (UUID) като обикновено поле
- **Запазваме**: id, code, issuedAt, usedAt

```java
@Entity
@Table(name = "tickets")
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private UUID subscriptionId;  // Вместо @OneToOne
    
    @Column(nullable = false, unique = true, length = 64)
    private String code;
    
    @Column(nullable = false)
    private LocalDateTime issuedAt;
    
    private LocalDateTime usedAt;
}
```

---

## 🔌 REST API Endpoints

### 1. POST `/api/tickets` - Създаване на билет
**Използва се от:** SubscriptionService.create() в Main приложението
```json
Request Body:
{
  "subscriptionId": "uuid",
  "userId": "uuid",        // за валидация
  "eventId": "uuid"        // за валидация
}

Response: 201 Created
{
  "id": "uuid",
  "code": "generated-code",
  "subscriptionId": "uuid",
  "issuedAt": "2025-01-01T10:00:00"
}
```

### 2. DELETE `/api/tickets/subscription/{subscriptionId}` - Изтриване на билет
**Използва се от:** SubscriptionService.deleteByUserAndEvent() в Main приложението
```
Response: 204 No Content
```

### 3. GET `/api/tickets/{code}` - Търсене по код
**Използва се от:** TicketController.renderTicketQr() в Main приложението
```json
Response: 200 OK
{
  "id": "uuid",
  "code": "code",
  "subscriptionId": "uuid",
  "issuedAt": "2025-01-01T10:00:00",
  "usedAt": null,
  "userId": "uuid",        // от subscription (за валидация)
  "eventId": "uuid"        // от subscription (за валидация)
}
```

### 4. GET `/api/tickets/user/{userId}` - Всички билети на потребител
**Използва се от:** EventService.getSubscribedEvents() в Main приложението
```json
Response: 200 OK
[
  {
    "id": "uuid",
    "code": "code",
    "subscriptionId": "uuid",
    "eventId": "uuid",
    "issuedAt": "2025-01-01T10:00:00"
  }
]
```

### 5. PUT `/api/tickets/{code}/validate` - Маркиране като използван (БОНУС функционалност)
**Използва се от:** Валидация на билет при влизане в събитие
```json
Response: 200 OK
{
  "id": "uuid",
  "code": "code",
  "usedAt": "2025-01-01T12:00:00"
}
```

---

## 🔄 Feign Client в Main Application

### Създаване на Feign Client:
```java
@FeignClient(name = "ticket-service", url = "http://localhost:8081")
public interface TicketServiceClient {
    
    @PostMapping("/api/tickets")
    TicketResponse createTicket(@RequestBody TicketCreateRequest request);
    
    @DeleteMapping("/api/tickets/subscription/{subscriptionId}")
    void deleteTicketBySubscriptionId(@PathVariable UUID subscriptionId);
    
    @GetMapping("/api/tickets/{code}")
    TicketResponse getTicketByCode(@PathVariable String code);
    
    @GetMapping("/api/tickets/user/{userId}")
    List<TicketResponse> getTicketsByUserId(@PathVariable UUID userId);
    
    @PutMapping("/api/tickets/{code}/validate")
    TicketResponse validateTicket(@PathVariable String code);
}
```

### DTOs за комуникация:
```java
// TicketCreateRequest
public class TicketCreateRequest {
    private UUID subscriptionId;
    private UUID userId;
    private UUID eventId;
}

// TicketResponse
public class TicketResponse {
    private UUID id;
    private String code;
    private UUID subscriptionId;
    private UUID eventId;      // от subscription
    private UUID userId;       // от subscription
    private LocalDateTime issuedAt;
    private LocalDateTime usedAt;
}
```

---

## 🗄️ База Данни

### Microservice база данни:
```properties
# ticket-service/src/main/resources/application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/ticket_service_db?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=123456
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

server.port=8081
```

### Main Application база данни (остава същата):
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/event_app_db?createDatabaseIfNotExist=true
```

---

## ✅ Изисквания - Проверка

### REST Microservice изисквания:
- ✅ **Поне 1 domain entity**: Ticket
- ✅ **Поне 2 POST/PUT/DELETE endpoints**: 
  - POST `/api/tickets` ✅
  - DELETE `/api/tickets/subscription/{subscriptionId}` ✅
  - PUT `/api/tickets/{code}/validate` ✅ (бонус)
- ✅ **Поне 1 GET endpoint**: 
  - GET `/api/tickets/{code}` ✅
  - GET `/api/tickets/user/{userId}` ✅
- ✅ **Feign Client комуникация**: TicketServiceClient в Main приложението
- ✅ **Отделна база данни**: ticket_service_db
- ✅ **Error handling**: 2 error handlers (built-in + custom)
- ✅ **Validation**: DTO validation
- ✅ **Logging**: Във всички functionalities
- ✅ **Testing**: Unit, Integration, API tests

### Valid Functionalities в Microservice:
1. **POST `/api/tickets`** - Създаване на билет при записване за събитие
2. **DELETE `/api/tickets/subscription/{subscriptionId}`** - Изтриване при отписване
3. **PUT `/api/tickets/{code}/validate`** - Маркиране като използван (бонус)

---

## 🔧 Технически Детайли

### Dependencies в ticket-service/pom.xml:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Dependencies в Main Application (добавяне на Feign):
```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <version>4.1.0</version>
</dependency>
```

### Конфигурация на Feign в Main Application:
```java
@SpringBootApplication
@EnableFeignClients
public class EventAppApplication {
    // ...
}
```

---

## 📝 Стъпки за Имплементация

### Фаза 1: Създаване на Microservice
1. ✅ Създаване на нов Spring Boot проект (ticket-service)
2. ✅ Конфигуриране на pom.xml
3. ✅ Създаване на Ticket entity (без Subscription връзка)
4. ✅ Създаване на TicketRepository
5. ✅ Създаване на TicketService (business logic)
6. ✅ Създаване на TicketController (REST endpoints)
7. ✅ Създаване на DTOs (Request/Response)
8. ✅ Конфигуриране на база данни

### Фаза 2: Error Handling & Validation
9. ✅ Създаване на Custom Exception (TicketNotFoundException)
10. ✅ Създаване на GlobalExceptionHandler
11. ✅ Добавяне на validation в DTOs
12. ✅ Добавяне на logging

### Фаза 3: Интеграция с Main Application
13. ✅ Добавяне на Feign Client dependency в Main
14. ✅ Създаване на TicketServiceClient (Feign interface)
15. ✅ Създаване на DTOs в Main за комуникация
16. ✅ Замяна на TicketService извиквания с Feign Client
17. ✅ Премахване на Ticket entity/repository/service от Main
18. ✅ Обновяване на TicketController в Main да използва Feign

### Фаза 4: Testing
19. ✅ Unit tests за TicketService
20. ✅ Integration tests за TicketRepository
21. ✅ API tests за TicketController (@WebMvcTest)

### Фаза 5: Scheduling & Caching (опционално)
22. ⚠️ Scheduled job за изчистване на стари билети (ако е нужно)
23. ⚠️ Caching за често използвани заявки (ако е нужно)

---

## 🎯 Промени в Main Application

### Файлове за премахване:
- ❌ `src/main/java/main/model/Ticket.java`
- ❌ `src/main/java/main/repository/TicketRepository.java`
- ❌ `src/main/java/main/service/TicketService.java`

### Файлове за модификация:
- ✏️ `SubscriptionService.java` - използва Feign Client вместо TicketService
- ✏️ `EventService.java` - използва Feign Client вместо TicketService
- ✏️ `TicketController.java` - използва Feign Client вместо TicketService
- ✏️ `pom.xml` - добавяне на Feign Client dependency

### Нови файлове:
- ➕ `TicketServiceClient.java` (Feign interface)
- ➕ `TicketCreateRequest.java` (DTO)
- ➕ `TicketResponse.java` (DTO)

---

## 🔍 Важни Бележки

1. **Subscription ID**: Microservice няма достъп до Subscription entity, затова използваме само subscriptionId като UUID
2. **Валидация**: Main приложението трябва да валидира, че subscriptionId съществува преди извикване на microservice
3. **Транзакции**: Транзакциите между Main и Microservice са distributed - трябва да се обработват грешките правилно
4. **Порт**: Microservice ще работи на порт 8081, Main на 8080
5. **QR Code Service**: Остава в Main приложението, защото се използва само за генериране на изображения

---

## ✅ Checklist за Завършване

- [ ] Ticket Service microservice създаден и работи
- [ ] Всички REST endpoints имплементирани
- [ ] Feign Client конфигуриран в Main приложението
- [ ] Main приложението използва Feign Client вместо директни service calls
- [ ] Отделни бази данни конфигурирани
- [ ] Error handling имплементиран (2 handlers)
- [ ] Validation имплементирана
- [ ] Logging добавен във всички functionalities
- [ ] Тестове написани (unit, integration, API)
- [ ] Двете приложения работят независимо
- [ ] README.md обновен с информация за microservice


# PulsePass — Sistema de Gestión de Eventos y Tickets

Aplicación Spring Boot para la gestión de eventos musicales, artistas, venues, usuarios y tickets.  
Este proyecto implementa las Fases 1, 2 y 3 del taller de Desarrollo de Software (UniCaribe).

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ (o usar `./mvnw` incluido) |
| Docker Desktop | 24+ (necesario para las pruebas de integración con Testcontainers) |
| PostgreSQL | 15+ (solo para ejecutar la aplicación en local) |

---

## Configuración de la base de datos (ejecución local)

1. Crea una base de datos PostgreSQL:

```sql
CREATE DATABASE pulsepassdb;
CREATE USER pulsepass WITH PASSWORD 'pulsepass';
GRANT ALL PRIVILEGES ON DATABASE pulsepassdb TO pulsepass;
```

2. Ajusta `src/main/resources/application.properties` si tus credenciales son distintas:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/pulsepassdb
spring.datasource.username=pulsepass
spring.datasource.password=pulsepass
```

3. Las migraciones de esquema se ejecutan automáticamente con Flyway al iniciar la aplicación.

---

## Ejecutar la aplicación

```bash
# Con el wrapper de Maven incluido
./mvnw spring-boot:run

# O con Maven instalado globalmente
mvn spring-boot:run
```

La API queda disponible en `http://localhost:8080`.

---

## Ejecutar las pruebas

### Pruebas de controladores (sin base de datos — requiere solo JDK)

Las pruebas de la capa de controladores usan `@WebMvcTest` y no necesitan PostgreSQL ni Docker:

```bash
mvn clean test -Dtest="*ControllerTest,GlobalExceptionHandlerTest" -DfailIfNoTests=false
```

### Pruebas de servicios (sin base de datos)

```bash
mvn clean test -Dtest="*ServiceImplTest" -DfailIfNoTests=false
```

### Pruebas de integración (requiere Docker en ejecución)

Las pruebas de repositorio usan Testcontainers para levantar un contenedor PostgreSQL automáticamente:

```bash
mvn clean test -Dtest="*IT,*PersistenceTest" -DfailIfNoTests=false
```

### Todas las pruebas de Fase 3 sin base de datos (controladores + servicios)

El siguiente comando ejecuta las 61 pruebas que no requieren PostgreSQL ni Docker:
36 de controladores (MockMvc) + 25 de servicios (unitarias con Mockito).

```bash
mvn clean test -Dtest="*ControllerTest,GlobalExceptionHandlerTest,*ServiceImplTest" -DfailIfNoTests=false
```

---

## Endpoints disponibles

### Venues — `/api/venues`

| Método | URL | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/venues/{code}` | Consultar venue por código | `200 OK` / `404` |
| GET | `/api/venues/active` | Listar venues activos | `200 OK` |

### Artists — `/api/artists`

| Método | URL | Descripción | Respuesta |
|---|---|---|---|
| GET | `/api/artists/{id}` | Consultar artista por ID | `200 OK` / `404` |
| GET | `/api/artists/by-stage-name?stageName=` | Buscar por nombre artístico | `200 OK` / `404` |
| GET | `/api/artists/active` | Listar artistas activos | `200 OK` |

### Events — `/api/events`

| Método | URL | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/events` | Crear evento | `201 Created` / `400` / `409` |
| GET | `/api/events/{eventCode}` | Consultar evento por código | `200 OK` / `404` |
| GET | `/api/events/published` | Listar eventos publicados | `200 OK` |
| PATCH | `/api/events/{eventCode}/publish` | Publicar evento | `200 OK` / `409` |
| POST | `/api/events/{eventCode}/artists/{artistId}` | Asociar artista a evento | `200 OK` / `404` / `409` |
| GET | `/api/events/by-artist?stageName=` | Buscar eventos por artista | `200 OK` |
| GET | `/api/events/{eventCode}/tickets/paid` | Tickets pagados de un evento | `200 OK` |

### Users — `/api/users`

| Método | URL | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/users` | Registrar usuario | `201 Created` / `400` / `409` |
| GET | `/api/users/by-email?email=` | Consultar usuario por email | `200 OK` / `404` |
| GET | `/api/users/by-username?username=` | Consultar usuario por username | `200 OK` / `404` |

### Tickets — `/api/tickets`

| Método | URL | Descripción | Respuesta |
|---|---|---|---|
| POST | `/api/tickets` | Comprar ticket | `201 Created` / `400` / `404` / `409` |
| GET | `/api/tickets/{ticketCode}` | Consultar ticket por código | `200 OK` / `404` |
| GET | `/api/tickets/by-user?email=` | Tickets de un usuario | `200 OK` |
| PATCH | `/api/tickets/{ticketCode}/cancel` | Cancelar ticket | `200 OK` / `409` |
| PATCH | `/api/tickets/{ticketCode}/use` | Marcar ticket como usado | `200 OK` / `409` |

---

## Estructura del proyecto

```
src/
├── main/java/com/pulsepass/pulsepass/
│   ├── controller/        ← Fase 3: REST Controllers
│   ├── service/           ← Fase 2: lógica de negocio
│   ├── repository/        ← Fase 1: acceso a datos
│   ├── entity/            ← Entidades JPA
│   ├── dto/
│   │   ├── request/       ← DTOs de entrada con Bean Validation
│   │   └── response/      ← DTOs de salida
│   ├── mapper/            ← MapStruct mappers
│   └── exception/         ← GlobalExceptionHandler + excepciones de dominio
└── test/java/com/pulsepass/pulsepass/
    ├── controller/        ← Pruebas con MockMvc (@WebMvcTest) — sin BD
    ├── service/           ← Pruebas unitarias de servicios — sin BD
    └── repository/        ← Pruebas de integración con Testcontainers
```

---

## Manejo de errores

Todos los errores retornan un `ErrorResponse` uniforme:

```json
{
  "timestamp": "2026-10-11T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Artist not found: 99",
  "details": {}
}
```

En errores de validación (`400`), el campo `details` contiene el mapa de campos con sus mensajes:

```json
{
  "timestamp": "2026-10-11T14:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": {
    "email": "Email must be valid",
    "eventCode": "Event code is required"
  }
}
```

---

## Resumen de pruebas de la Fase 3

### Pruebas de controladores — 36 tests (sin base de datos)

| Clase | Tests | Tipo |
|---|---|---|
| `VenueControllerTest` | 3 | MockMvc / @WebMvcTest |
| `ArtistControllerTest` | 4 | MockMvc / @WebMvcTest |
| `EventControllerTest` | 9 | MockMvc / @WebMvcTest |
| `UserControllerTest` | 5 | MockMvc / @WebMvcTest |
| `TicketControllerTest` | 11 | MockMvc / @WebMvcTest |
| `GlobalExceptionHandlerTest` | 4 | MockMvc / @WebMvcTest |
| **Subtotal** | **36** | Sin base de datos |

### Pruebas de servicios — 25 tests (sin base de datos)

| Clase | Tests | Tipo |
|---|---|---|
| `EventServiceImplTest` | 9 | Unitaria / Mockito |
| `TicketServiceImplTest` | 12 | Unitaria / Mockito |
| `UserServiceImplTest` | 4 | Unitaria / Mockito |
| **Subtotal** | **25** | Sin base de datos |

### Total de pruebas sin infraestructura: **61 tests**

### Pruebas de integración (requieren Docker)

| Clase | Tipo |
|---|---|
| `EventRepositoryIT`, `TicketRepositoryIT`, etc. | @DataJpaTest + Testcontainers |
| `VenuePersistenceTest` | @SpringBootTest + PostgreSQL |

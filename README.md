# AS241S5_AEJ_37-be

**Microservicio Spring WebFlux + R2DBC (PostgreSQL)** que consume 2 APIs de Inteligencia Artificial, captura los resultados y los almacena en una base de datos cloud (Neon).

---

## 1. APIs de Inteligencia Artificial

<img src="https://imagga.com/static/images/imagga_logo.png" align="right" style="width: 180px"/>

### API 1 — Imagga (Detector de objetos en imágenes)
- **Fuente:** [imagga.com](https://imagga.com)
- **Descripción:** Analiza una imagen a partir de su URL y retorna los objetos/tags detectados con nivel de confianza usando visión artificial.
- **Autenticación:** Basic Auth (api-key + api-secret)
- **Endpoint propio:** `POST /v1/api/pictocaption/describe`
- **Body:** `{ "imageUrl": "https://..." }`

<img src="https://openrouter.ai/favicon.ico" align="right" style="width: 60px"/>

### API 2 — GLM 4.5 Air via OpenRouter (IA general, free)
- **Fuente:** [openrouter.ai/z-ai/glm-4.5-air:free](https://openrouter.ai/z-ai/glm-4.5-air:free)
- **Descripción:** Modelo de lenguaje de propósito general capaz de responder preguntas, resumir texto y generar contenido.
- **Autenticación:** Bearer token (api-key de OpenRouter)
- **Endpoint propio:** `POST /v1/api/glm/chat`
- **Body:** `{ "prompt": "Tu pregunta aquí" }`

---

## 2. Herramientas y versiones

<img src="https://miro.medium.com/v2/resize:fit:716/1*98O4Gb5HLSlmdUkKg1DP1Q.png" align="right" style="height:60px; width: 200px"/>

- Java: JDK 17
- IDE: IntelliJ IDEA | Visual Studio Code | Codespace
- Maven: Apache Maven 3.x
- Framework: Spring Boot 3.3.3
- Base de datos cloud: Neon (PostgreSQL)
- Swagger: SpringDoc OpenAPI 2.6.0

---

## 3. Maven Dependencias

<img src="https://upload.wikimedia.org/wikipedia/commons/thumb/5/52/Apache_Maven_logo.svg/1280px-Apache_Maven_logo.svg.png" align="right" style="width: 200px"/>

- spring-boot-starter-webflux
- spring-boot-starter-data-r2dbc
- r2dbc-postgresql
- lombok
- reactor-test
- springdoc-openapi-starter-webflux-ui

## Dependencias Spring WebFlux + PostgreSQL (SQL)

Spring WebFlux | Data R2DBC | Project Reactor | R2DBC PostgreSQL

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-r2dbc</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>r2dbc-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

## Dependencias Swagger para Spring WebFlux

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
    <version>2.6.0</version>
</dependency>
```

---

## 4. Endpoints disponibles

### Imagga — API IA 1
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/api/pictocaption/describe` | Analizar imagen por URL y guardar resultado |
| GET | `/v1/api/pictocaption/history` | Ver historial de consultas guardadas |

### GLM 4.5 Air — API IA 2
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/v1/api/glm/chat` | Enviar prompt al modelo y guardar respuesta |
| GET | `/v1/api/glm/history` | Ver historial de consultas guardadas |

---

## 5. Configuración

Todas las credenciales (base de datos y APIs IA) se configuran en `src/main/resources/application.yml`.

```yaml
ai:
  imagga:
    url: https://api.imagga.com/v2/tags
    api-key: YOUR_IMAGGA_API_KEY
    api-secret: YOUR_IMAGGA_API_SECRET
  glm:
    url: https://openrouter.ai/api/v1/chat/completions
    api-key: YOUR_OPENROUTER_API_KEY
    model: z-ai/glm-4.5-air:free
```

---

## 6. Swagger UI

Disponible en: `http://localhost:8082/swagger-ui.html`

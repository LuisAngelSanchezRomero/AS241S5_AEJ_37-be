package pe.edu.vallegrande.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import pe.edu.vallegrande.app.model.AiResult;
import pe.edu.vallegrande.app.repository.AiResultRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class GlmServiceImpl {

    private final AiResultRepository aiResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.glm.url}")
    private String apiUrl;

    @Value("${ai.glm.api-key}")
    private String apiKey;

    @Value("${ai.glm.model}")
    private String model;

    public GlmServiceImpl(AiResultRepository aiResultRepository) {
        this.aiResultRepository = aiResultRepository;
    }

    // Envía un prompt al modelo GLM 4.5 Air y guarda la respuesta en BD
    public Mono<AiResult> chat(String prompt) {
        log.info("Consultando GLM con prompt: {}", prompt);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        // Se crea el WebClient aquí para que los @Value ya estén inyectados
        // Tiempo de 60s
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(err -> log.error("Error de OpenRouter: {}", err))
                                .flatMap(err -> Mono.error(new RuntimeException("OpenRouter error: " + err))))
                .bodyToMono(String.class)
                .flatMap(response -> {
                    String content = extractContent(response); // Extrae solo el texto de la respuesta antes de guardar
                    AiResult record = new AiResult();
                    record.setApiName("GLM");
                    record.setInputData(prompt);
                    record.setResult(content);
                    record.setCreatedAt(LocalDateTime.now());
                    return aiResultRepository.save(record);
                });
    }

    // Extrae el campo content del primer choice de la respuesta de OpenRouter
    private String extractContent(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            return root.path("choices").get(0).path("message").path("content").asText(); // Navega: choices[0].message.content
        } catch (Exception e) {
            log.warn("No se pudo parsear respuesta de GLM, guardando raw: {}", e.getMessage());
            return rawJson;
        }
    }

    // Retorna todos los resultados guardados de GLM (solo activos)
    public Flux<AiResult> getHistory() {
        return aiResultRepository.findByApiName("GLM")
                .filter(record -> "Activo".equals(record.getActive()));
    }

    // Actualiza una consulta existente y vuelve a consultar la API
    public Mono<AiResult> updateAndReconsult(Long id, String newPrompt) {
        log.info("Actualizando y re-consultando GLM ID: {} con nuevo prompt: {}", id, newPrompt);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Activo".equals(record.getActive()) && "GLM".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o inactivo")))
            .flatMap(existingRecord -> {
                // Hacer nueva consulta a GLM API
                return consultGlmApi(newPrompt)
                    .flatMap(newResult -> {
                        // Actualizar TODOS los campos
                        existingRecord.setInputData(newPrompt);
                        existingRecord.setResult(newResult);
                        existingRecord.setCreatedAt(LocalDateTime.now());
                        return aiResultRepository.save(existingRecord);
                    });
            });
    }

    // Borrado lógico de un registro
    public Mono<AiResult> deleteRecord(Long id) {
        log.info("Eliminando (borrado lógico) registro GLM ID: {}", id);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Activo".equals(record.getActive()) && "GLM".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o ya eliminado")))
            .flatMap(record -> {
                record.setActive("Inactivo");
                return aiResultRepository.save(record);
            });
    }

    // Restaurar un registro eliminado
    public Mono<AiResult> restoreRecord(Long id) {
        log.info("Restaurando registro GLM ID: {}", id);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Inactivo".equals(record.getActive()) && "GLM".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o no está eliminado")))
            .flatMap(record -> {
                record.setActive("Activo");
                return aiResultRepository.save(record);
            });
    }

    // Obtener historial de registros eliminados
    public Flux<AiResult> getDeletedHistory() {
        return aiResultRepository.findByApiName("GLM")
                .filter(record -> "Inactivo".equals(record.getActive()));
    }

    // Método auxiliar para separar la lógica de consulta API
    private Mono<String> consultGlmApi(String prompt) {
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(60));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build()
                .post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(err -> {
                                    if (res.statusCode().value() == 429) {
                                        log.error("Rate limit alcanzado en OpenRouter: {}", err);
                                    } else {
                                        log.error("Error de OpenRouter: {}", err);
                                    }
                                })
                                .flatMap(err -> {
                                    if (res.statusCode().value() == 429) {
                                        return Mono.error(new RuntimeException("Rate limit alcanzado. Intente nuevamente en unos minutos."));
                                    } else {
                                        return Mono.error(new RuntimeException("OpenRouter error: " + err));
                                    }
                                }))
                .bodyToMono(String.class)
                .map(this::extractContent); // Solo extrae el contenido, no guarda
    }
}

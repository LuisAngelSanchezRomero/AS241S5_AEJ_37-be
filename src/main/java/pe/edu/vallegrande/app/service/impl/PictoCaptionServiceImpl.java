package pe.edu.vallegrande.app.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.vallegrande.app.model.AiResult;
import pe.edu.vallegrande.app.repository.AiResultRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class PictoCaptionServiceImpl {

    private final AiResultRepository aiResultRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${ai.imagga.url}")
    private String apiUrl;

    @Value("${ai.imagga.api-key}")
    private String apiKey;

    @Value("${ai.imagga.api-secret}")
    private String apiSecret;

    public PictoCaptionServiceImpl(AiResultRepository aiResultRepository) {
        this.aiResultRepository = aiResultRepository;
    }

    // Envía una URL de imagen a Imagga para detectar objetos y guarda el resultado en BD
    public Mono<AiResult> describeImage(String imageUrl) {
        log.info("Consultando Imagga con imagen: {}", imageUrl);

        String credentials = Base64.getEncoder()
                .encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));

        return WebClient.builder().build()
                .get()
                .uri(apiUrl + "?image_url=" + imageUrl)
                .header("Authorization", "Basic " + credentials)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(err -> log.error("Error de Imagga: {}", err))
                                .flatMap(err -> Mono.error(new RuntimeException("Imagga error: " + err))))
                .bodyToMono(String.class)
                .flatMap(response -> {
                    String formatted = extractAndFormatTags(response); // Extrae y formatea los tags antes de guardar
                    AiResult record = new AiResult();
                    record.setApiName("Imagga");
                    record.setInputData(imageUrl);
                    record.setResult(formatted);
                    record.setCreatedAt(LocalDateTime.now());
                    return aiResultRepository.save(record);
                });
    }

    private String extractAndFormatTags(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode tags = root.path("result").path("tags");

            List<String> lines = new ArrayList<>();
            for (JsonNode tag : tags) {
                String en = tag.path("tag").path("en").asText();
                double confidence = tag.path("confidence").asDouble();
                lines.add(en + " (" + String.format("%.1f", confidence) + "%)");
            }

            return String.join(", ", lines);
        } catch (Exception e) {
            log.warn("No se pudo parsear respuesta de Imagga, guardando raw: {}", e.getMessage());
            return rawJson;
        }
    }

    // Retorna todos los resultados guardados de Imagga (solo activos)
    public Flux<AiResult> getHistory() {
        return aiResultRepository.findByApiName("Imagga")
                .filter(record -> "Activo".equals(record.getActive()));
    }

    // Actualiza una consulta existente y vuelve a consultar la API
    public Mono<AiResult> updateAndReconsult(Long id, String newImageUrl) {
        log.info("Actualizando y re-consultando Imagga ID: {} con nueva URL: {}", id, newImageUrl);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Activo".equals(record.getActive()) && "Imagga".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o inactivo")))
            .flatMap(existingRecord -> {
                // Hacer nueva consulta a Imagga API
                return consultImaggaApi(newImageUrl)
                    .flatMap(newResult -> {
                        // Actualizar TODOS los campos
                        existingRecord.setInputData(newImageUrl);
                        existingRecord.setResult(newResult);
                        existingRecord.setCreatedAt(LocalDateTime.now());
                        return aiResultRepository.save(existingRecord);
                    });
            });
    }

    // Borrado lógico de un registro
    public Mono<AiResult> deleteRecord(Long id) {
        log.info("Eliminando (borrado lógico) registro Imagga ID: {}", id);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Activo".equals(record.getActive()) && "Imagga".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o ya eliminado")))
            .flatMap(record -> {
                record.setActive("Inactivo");
                return aiResultRepository.save(record);
            });
    }

    // Restaurar un registro eliminado
    public Mono<AiResult> restoreRecord(Long id) {
        log.info("Restaurando registro Imagga ID: {}", id);
        
        return aiResultRepository.findById(id)
            .filter(record -> "Inactivo".equals(record.getActive()) && "Imagga".equals(record.getApiName()))
            .switchIfEmpty(Mono.error(new RuntimeException("Registro no encontrado o no está eliminado")))
            .flatMap(record -> {
                record.setActive("Activo");
                return aiResultRepository.save(record);
            });
    }

    // Obtener historial de registros eliminados
    public Flux<AiResult> getDeletedHistory() {
        return aiResultRepository.findByApiName("Imagga")
                .filter(record -> "Inactivo".equals(record.getActive()));
    }

    // Método auxiliar para separar la lógica de consulta API
    private Mono<String> consultImaggaApi(String imageUrl) {
        String credentials = Base64.getEncoder()
                .encodeToString((apiKey + ":" + apiSecret).getBytes(StandardCharsets.UTF_8));

        return WebClient.builder().build()
                .get()
                .uri(apiUrl + "?image_url=" + imageUrl)
                .header("Authorization", "Basic " + credentials)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        res -> res.bodyToMono(String.class)
                                .doOnNext(err -> log.error("Error de Imagga: {}", err))
                                .flatMap(err -> Mono.error(new RuntimeException("Imagga error: " + err))))
                .bodyToMono(String.class)
                .map(this::extractAndFormatTags); // Solo extrae y formatea, no guarda
    }
}

package pe.edu.vallegrande.app.service.impl;

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
        // Timeout de 60s porque los modelos free de OpenRouter pueden tardar
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
                    AiResult record = new AiResult();
                    record.setApiName("GLM");
                    record.setInputData(prompt);
                    record.setResult(response);
                    record.setCreatedAt(LocalDateTime.now());
                    return aiResultRepository.save(record);
                });
    }

    // Retorna todos los resultados guardados de GLM
    public Flux<AiResult> getHistory() {
        return aiResultRepository.findByApiName("GLM");
    }
}

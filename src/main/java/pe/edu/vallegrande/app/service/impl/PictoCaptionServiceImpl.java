package pe.edu.vallegrande.app.service.impl;

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
import java.util.Base64;

@Slf4j
@Service
public class PictoCaptionServiceImpl {

    private final AiResultRepository aiResultRepository;

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

        // Imagga usa Basic Auth con api-key:api-secret en Base64
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
                    AiResult record = new AiResult();
                    record.setApiName("Imagga");
                    record.setInputData(imageUrl);
                    record.setResult(response);
                    record.setCreatedAt(LocalDateTime.now());
                    return aiResultRepository.save(record);
                });
    }

    // Retorna todos los resultados guardados de Imagga
    public Flux<AiResult> getHistory() {
        return aiResultRepository.findByApiName("Imagga");
    }
}

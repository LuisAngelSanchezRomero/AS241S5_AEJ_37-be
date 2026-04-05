package pe.edu.vallegrande.app.rest;

import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.model.AiResult;
import pe.edu.vallegrande.app.model.dto.ChatRequest;
import pe.edu.vallegrande.app.service.impl.GlmServiceImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/glm")
public class GlmRest {

    private final GlmServiceImpl service;

    public GlmRest(GlmServiceImpl service) {
        this.service = service;
    }

    // Recibe un prompt y retorna la respuesta generada por GLM 4.5 Air
    @PostMapping("/chat")
    public Mono<AiResult> chat(@RequestBody ChatRequest body) {
        return service.chat(body.getPrompt());
    }

    // Retorna el historial de consultas guardadas de GLM
    @GetMapping("/history")
    public Flux<AiResult> history() {
        return service.getHistory();
    }
}

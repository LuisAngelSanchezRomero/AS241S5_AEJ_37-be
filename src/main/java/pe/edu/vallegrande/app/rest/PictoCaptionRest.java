package pe.edu.vallegrande.app.rest;

import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.app.model.AiResult;
import pe.edu.vallegrande.app.model.dto.ImageRequest;
import pe.edu.vallegrande.app.service.impl.PictoCaptionServiceImpl;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/v1/api/pictocaption")
public class PictoCaptionRest {

    private final PictoCaptionServiceImpl service;

    public PictoCaptionRest(PictoCaptionServiceImpl service) {
        this.service = service;
    }

    // Recibe una URL de imagen y retorna la descripción generada por la IA
    @PostMapping("/describe")
    public Mono<AiResult> describe(@RequestBody ImageRequest body) {
        return service.describeImage(body.getImageUrl());
    }

    // Retorna el historial de consultas guardadas de PictoCaption
    @GetMapping("/history")
    public Flux<AiResult> history() {
        return service.getHistory();
    }
}

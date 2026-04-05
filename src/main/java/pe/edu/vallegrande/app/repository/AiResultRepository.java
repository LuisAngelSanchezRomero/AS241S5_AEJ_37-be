package pe.edu.vallegrande.app.repository;

import pe.edu.vallegrande.app.model.AiResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AiResultRepository extends ReactiveCrudRepository<AiResult, Long> {

    Flux<AiResult> findByApiName(String apiName);
}

package pe.edu.vallegrande.app.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table(name = "ai_result")
public class AiResult {

    @Id
    private Long id;

    @Column("api_name")
    private String apiName;

    @Column("input_data")
    private String inputData;

    @Column("result")
    private String result;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("active")
    private String active = "Activo";
}

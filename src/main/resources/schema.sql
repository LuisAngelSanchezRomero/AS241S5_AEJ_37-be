-- Tabla para almacenar resultados de las APIs de IA
CREATE TABLE IF NOT EXISTS ai_result (
    id SERIAL PRIMARY KEY NOT NULL UNIQUE,
    api_name VARCHAR(50) NOT NULL,
    input_data TEXT NOT NULL,
    result TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    active VARCHAR(10) DEFAULT 'Activo'
);
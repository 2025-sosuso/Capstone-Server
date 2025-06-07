package com.knu.sosuso.capstone.swagger.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Capstone API",
                version = "1.0.0",
                description = "우리들의 API들 .. ^0^"
        ),
        servers = {
                @Server(url = "https://knu-sosuso.com", description = "Production server"),
                @Server(url = "http://localhost:8080", description = "Local development server")
        }
)
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components());
    }
}
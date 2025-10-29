package com.example.moneymarket.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for OpenAPI/Swagger
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI moneyMarketOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Money Market API")
                        .description("Core Banking System - Money Market Module")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Money Market Team")
                                .email("support@moneymarket.com"))
                        .license(new License().name("Proprietary")))
                .externalDocs(new ExternalDocumentation()
                        .description("Money Market Module Documentation")
                        .url("https://example.com/docs"));
    }
}

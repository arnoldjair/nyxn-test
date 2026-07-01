package com.nyxn.ecommerce.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("NYXN Ecommerce Product API")
                .version("1.0")
                .description("RESTful API for e-commerce product catalog management")
                .contact(new Contact()
                    .name("NYXN Team")
                    .email("dev@nyxn.com")
                )
            );
    }
}

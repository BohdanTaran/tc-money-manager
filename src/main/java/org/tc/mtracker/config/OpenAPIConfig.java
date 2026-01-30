package org.tc.mtracker.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI mtServiceAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("MT Service API")
                        .description("REST API Documentation for MT Service")
                );
    }
}

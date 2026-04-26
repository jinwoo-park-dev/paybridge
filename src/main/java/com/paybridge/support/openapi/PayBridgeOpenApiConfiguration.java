package com.paybridge.support.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PayBridgeOpenApiConfiguration {

    @Bean
    public OpenAPI payBridgeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayBridge API")
                        .version("1.0.0")
                        .description("Documented JSON endpoints for local development and operator tooling. Server rendered pages stay outside the documented JSON API contract.")
                        .contact(new Contact().name("PayBridge").url("https://github.com/jinwoo-park-dev/paybridge"))
                        .license(new License().name("MIT").url("https://github.com/jinwoo-park-dev/paybridge/blob/main/LICENSE.txt")));
    }
}

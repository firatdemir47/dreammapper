package com.dreammapper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI customOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("DreamMapper API")
						.version("1.0.0")
						.description("Yapay zekâ destekli rüya analiz uygulaması API dokümantasyonu")
						.contact(new Contact()
								.name("DreamMapper Team")
								.email("info@dreammapper.com"))
						.license(new License()
								.name("Apache 2.0")
								.url("https://www.apache.org/licenses/LICENSE-2.0.html")))
				.addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
				.components(new io.swagger.v3.oas.models.Components()
						.addSecuritySchemes("Bearer Authentication",
								new SecurityScheme()
										.type(SecurityScheme.Type.HTTP)
										.scheme("bearer")
										.bearerFormat("JWT")
										.description("JWT token ile kimlik doğrulama")));
	}
}


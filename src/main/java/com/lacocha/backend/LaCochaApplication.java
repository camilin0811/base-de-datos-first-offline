package com.lacocha.backend;

import java.util.Locale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LaCochaApplication {

    public static void main(String[] args) {
        // Mensajes de validación en español aunque el servidor de Render esté en inglés
        Locale.setDefault(Locale.forLanguageTag("es"));
        SpringApplication.run(LaCochaApplication.class, args);
    }
}

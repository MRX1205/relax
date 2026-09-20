package com.relax.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer serializeLongAsString() {
        return builder -> {
            SimpleModule module = new SimpleModule("long-as-string");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            builder.addModule(module);
        };
    }
}

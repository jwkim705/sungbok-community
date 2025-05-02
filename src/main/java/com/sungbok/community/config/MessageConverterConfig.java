package com.sungbok.community.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sungbok.community.common.xss.HtmlCharacterEscapes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.nio.charset.StandardCharsets;

@Configuration
public class MessageConverterConfig {

    @Bean
    public MappingJackson2HttpMessageConverter htmlEscapedJacksonConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getFactory().setCharacterEscapes(new HtmlCharacterEscapes());
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter =new MappingJackson2HttpMessageConverter(objectMapper);
        converter.setDefaultCharset(StandardCharsets.UTF_8);
        return converter;
    }

}

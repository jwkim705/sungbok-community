package com.sungbok.community.config;

import com.sungbok.community.common.xss.HtmlCharacterEscapes;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessageConverterConfig {

    /**
     * Jackson 3 ObjectMapper 설정
     * - XSS 방어를 위한 HTML 이스케이핑 적용
     * - JSR-310 (Java 8 Date/Time) 지원은 Jackson 3에서 자동 포함
     * - Spring Boot가 이 ObjectMapper를 자동으로 MessageConverter에 적용
     */
    @Bean
    public ObjectMapper objectMapper() {
        // Jackson 3: JsonFactory builder로 CharacterEscapes 설정
        JsonFactory factory = JsonFactory.builder()
                .characterEscapes(new HtmlCharacterEscapes())
                .build();

        return new ObjectMapper(factory);
    }

}

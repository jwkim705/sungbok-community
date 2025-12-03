package com.sungbok.community.config;

import lombok.RequiredArgsConstructor;
import org.jooq.conf.ExecuteWithoutWhere;
import org.jooq.conf.RenderKeywordCase;
import org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JooqConfig {

    @Bean
    public DefaultConfigurationCustomizer jooqDefaultConfigurationCustomizer() {
        return c -> {
            c.settings()
                .withRenderFormatted(true)
                .withRenderSchema(false)
//                .withRenderQuotedNames(RenderQuotedNames.NEVER) //쿼터(백틱) 제거 - $name() 메서드와 충돌
                .withBatchSize(2000) //한번에 실행 쿼리 제한 2000
                .withRenderKeywordCase(RenderKeywordCase.UPPER) //SQL 키워드 대문자 (SELECT, FROM)
//                .withRenderNameCase(RenderNameCase.UPPER) //테이블/컬럼명 대문자 변환 - 데이터베이스와 충돌로 주석처리
//                .withRenderTable(RenderTable.WHEN_MULTIPLE_TABLES) //여러 테이블 JOIN시에만 테이블명 붙임 - alias에 점(.)이 들어가서 MySQL 오류
                .withExecuteUpdateWithoutWhere(ExecuteWithoutWhere.THROW) //WHERE절 없이 UPDATE시 에러
                .withExecuteDeleteWithoutWhere(ExecuteWithoutWhere.THROW) //WHERE절 없이 DELETE시 에러
                .withQueryTimeout(60) // SQL 실행 타임아웃
                .withRenderGroupConcatMaxLenSessionVariable(false); //Group Concat 사용시 세션 설정 수정 안함
            ;
        };
    }

}
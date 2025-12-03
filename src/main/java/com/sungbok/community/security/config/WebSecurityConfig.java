package com.sungbok.community.security.config;

import com.sungbok.community.config.CorsConfig;
import com.sungbok.community.security.handler.*;
import com.sungbok.community.security.provider.AuthProvider;
import com.sungbok.community.security.provider.CustomAuthenticationProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final CorsConfig corsConfig;
    private final AuthProvider authProvider;
    private final CustomAuthenticationEntryPointHandler customAuthenticationEntryPointHandler;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAuthenticationFailHandler customAuthenticationFailHandler;
//    private final Oauth2UserDetailsServiceImpl oauth2UserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors ->
                        cors.configurationSource(corsConfig.corsConfigurationSource())
                )
                .authorizeHttpRequests(authorize -> authorize
                        // 공개 접근 엔드포인트
                        .requestMatchers("/health-check").permitAll()
                        .requestMatchers("/users/signup").permitAll()
                        .requestMatchers("/posts").permitAll()  // GET /api/posts - 게시글 목록 조회

                        // 로그인/로그아웃 엔드포인트
                        .requestMatchers("/login", "/logout").permitAll()

                        // API 문서 (이미 ignoreList에 있지만 명시)
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger.html").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()

                        // 나머지 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                .headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer
                        .xssProtection(xXssConfig ->
                                xXssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // X-XSS-Protection: 1; mode=block;
                        .contentTypeOptions(withDefaults()) // X-Content-Type-Options: nosniff
                        .cacheControl(withDefaults()) // Cache-Control: no-cache, no-store, max-age=0, must-revalidate
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // X-Frame-Options: SAMEORIGIN
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                )
//                .oauth2Login(oauth2 -> oauth2
//                        .redirectionEndpoint(redirection -> redirection
//                                .baseUri("/login/oauth2/code/**"))
//                        .userInfoEndpoint(userInfo -> userInfo
//                                .userService(oauth2UserDetailsService))
//                        .successHandler(customAuthenticationSuccessHandler)
//                        .failureHandler(customAuthenticationFailHandler)
//                )
                .formLogin(configurer ->
                        configurer
                                .usernameParameter("email")
                                .passwordParameter("password")
                                .loginProcessingUrl("/login")
                                .successHandler(customAuthenticationSuccessHandler)
                                .failureHandler(customAuthenticationFailHandler)
                )
                .logout(configurer ->
                        configurer
                                .logoutUrl("/logout")
                                .logoutSuccessHandler(customLogoutSuccessHandler)
                                .invalidateHttpSession(true)
                )
                .sessionManagement(session -> // 세션 고정 보호
                        session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS) // 생성 정책
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::newSession)
                        .maximumSessions(1) // 동시 세션 제어
                        .maxSessionsPreventsLogin(true) // 최대 세션 도달 시 로그인 차단
                )
                .exceptionHandling(handler ->
                        handler
                                .authenticationEntryPoint(customAuthenticationEntryPointHandler)
                                .accessDeniedHandler(customAccessDeniedHandler)
                )
                .build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return webSecurity ->
                webSecurity
                        .ignoring()
                        .requestMatchers(authProvider.ignoreListDefaultEndpoints())
                        .requestMatchers(authProvider.whiteListDefaultEndpoints());
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        ProviderManager providerManager = (ProviderManager) authenticationConfiguration.getAuthenticationManager();
        providerManager.getProviders().add(this.customAuthenticationProvider);
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

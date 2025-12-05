package com.sungbok.community.security.config;

import static org.springframework.security.config.Customizer.withDefaults;

import com.sungbok.community.config.CorsConfig;
import com.sungbok.community.security.handler.CustomAccessDeniedHandler;
import com.sungbok.community.security.handler.CustomAuthenticationFailHandler;
import com.sungbok.community.security.handler.CustomAuthenticationSuccessHandler;
import com.sungbok.community.security.handler.CustomLogoutSuccessHandler;
import com.sungbok.community.security.jwt.JwtAuthenticationEntryPoint;
import com.sungbok.community.security.jwt.JwtAuthenticationFilter;
import com.sungbok.community.security.provider.AuthProvider;
import com.sungbok.community.security.provider.CustomAuthenticationProvider;
import com.sungbok.community.security.service.impl.Oauth2UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfig {

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    private final CorsConfig corsConfig;
    private final AuthProvider authProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final CustomAuthenticationProvider customAuthenticationProvider;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final CustomAuthenticationFailHandler customAuthenticationFailHandler;
    private final Oauth2UserDetailsServiceImpl oauth2UserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .cors(cors ->
                        cors.configurationSource(corsConfig.corsConfigurationSource())
                )
                // JWT 인증 필터 (SessionHeaderFilter 대체)
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                )
                .authorizeHttpRequests(authorize -> {
                        // 공개 접근 엔드포인트
                        authorize.requestMatchers("/health-check").permitAll();
                        authorize.requestMatchers("/users/signup").permitAll();
                        authorize.requestMatchers(HttpMethod.GET, "/posts", "/posts/**").permitAll(); // GET /api/posts, /api/posts/{postId} - 게시글 조회

                        // JWT 인증 엔드포인트
                        authorize.requestMatchers("/auth/**").permitAll();

                        // 로그인/로그아웃 엔드포인트
                        authorize.requestMatchers("/login", "/logout").permitAll();

                        // OAuth2 로그인 엔드포인트
                        authorize.requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll();

                        // API 문서
                        authorize.requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**").permitAll();

                        // H2 콘솔은 개발/테스트 환경에서만 허용
                        if ("local".equals(activeProfile) || "test".equals(activeProfile)) {
                            authorize.requestMatchers("/h2-console/**").permitAll();
                        }

                        // 나머지 모든 요청은 인증 필요
                        authorize.anyRequest().authenticated();
                })
                .headers(httpSecurityHeadersConfigurer -> httpSecurityHeadersConfigurer
                        .xssProtection(xXssConfig ->
                                xXssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                        // X-XSS-Protection: 1; mode=block;
                        .contentTypeOptions(withDefaults()) // X-Content-Type-Options: nosniff
                        .cacheControl(withDefaults()) // Cache-Control: no-cache, no-store, max-age=0, must-revalidate
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin) // X-Frame-Options: SAMEORIGIN
                        .httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable)
                )
                .oauth2Login(oauth2 -> oauth2
                        .redirectionEndpoint(redirection -> redirection
                                .baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(oauth2UserDetailsService))
                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailHandler)
                )
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
                                .logoutUrl("/logout") // POST 메서드만 허용 (Spring Security 기본값)
                                .logoutSuccessHandler(customLogoutSuccessHandler)
                                .invalidateHttpSession(false) // JWT는 stateless이므로 세션 무효화 불필요
                                .deleteCookies() // 쿠키 삭제 불필요 (JWT는 헤더 기반)
                )
                .sessionManagement(session ->
                        // JWT 기반 인증: STATELESS (세션 생성하지 않음)
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(handler ->
                        handler
                                .authenticationEntryPoint(jwtAuthenticationEntryPoint) // JWT Entry Point
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
    public AuthenticationManager authenticationManager() {
        ProviderManager providerManager = (ProviderManager) authenticationConfiguration.getAuthenticationManager();
        providerManager.getProviders().add(this.customAuthenticationProvider);
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

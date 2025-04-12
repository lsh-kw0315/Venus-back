package com.ll.server.global.security.config;

import com.ll.server.global.redis.RedisService;
import com.ll.server.global.security.custom.CustomOAuth2UserService;
import com.ll.server.global.security.custom.CustomUserDetailsService;
import com.ll.server.global.security.filter.JwtAuthenticationFilter;
import com.ll.server.global.security.filter.JwtAuthorizationFilter;
import com.ll.server.global.security.handler.OAuth2SuccessHandler;
import com.ll.server.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService customUserDetailsService;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final RedisService redisService;

    /*
    @Bean
    @Order(0)
    public SecurityFilterChain oauth2FilterChain (HttpSecurity http) throws Exception {

        http
                .securityMatcher("/oauth/**","/oauth2/**", "/login/**", "/logout/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers->headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        http.authorizeHttpRequests(auth -> auth     // 인가 (Authorization) 설정
                .anyRequest().permitAll());

        http.oauth2Login(oauth2 -> oauth2           // OAuth2 로그인 설정
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .authorizationEndpoint(auth -> auth.baseUri("/oauth2/authorization")) // 이 URL을 통해 OAuth2 제공자에 연결
                .redirectionEndpoint(rd->rd.baseUri("/oauth2/callback/kakao")) //이 URL을 통해 OAuth2 제공자가 인가 코드를 제공
        );

            // JWT Filter 추가
                http.addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

        http
                .securityMatcher("/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers->headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

        http.authorizeHttpRequests(auth -> auth     // 인가 (Authorization) 설정
                .requestMatchers("/api-test", "/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/member/signup", "/api/member/login").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/publisher/**").hasAnyRole("PUBLISHER", "ADMIN")
                .anyRequest().authenticated());

        http.logout(logout -> logout
                .logoutRequestMatcher(new AntPathRequestMatcher("/api/v1/member/logout"))
                .clearAuthentication(true) // 인증 정보 제거
                .invalidateHttpSession(true) // 세션 무효화
                .deleteCookies("accessToken", "refreshToken") // 쿠키 삭제
                .permitAll());

        http    // JWT Filter 추가
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

     */

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth     // 인가 (Authorization) 설정
                        .requestMatchers("/api/v1/member/signup", "/api/v1/member/login", "/login/**","/oauth2/**", "/api/v1/member/auth", "/h2-console", "/h2-console/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/publisher/**").hasAnyRole("PUBLISHER", "ADMIN")
                        .anyRequest().authenticated()
                )

                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/api/v1/member/logout"))
                        .clearAuthentication(true) // 인증 정보 제거
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("accessToken", "refreshToken") // 쿠키 삭제
                        //.logoutSuccessUrl("http://localhost:5173/logout")
                        .permitAll()
                )

                .oauth2Login(oauth2 -> oauth2           // OAuth2 로그인 설정
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                )

                // JWT Filter 추가
                .addFilterBefore(jwtAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter(), JwtAuthorizationFilter.class);

        return http.build();

    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() throws Exception {
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, authenticationManager(authenticationConfiguration));
        filter.setAuthenticationManager(authenticationManager(authenticationConfiguration));
        return filter;

    }

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtUtil, customUserDetailsService, redisService);
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173", "http://localhost:8080")); // 프론트 URL 허용
        configuration.setAllowedMethods(Arrays.asList("HEAD", "POST", "GET", "DELETE", "PUT", "PATCH", "OPTION"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true); // 쿠키 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

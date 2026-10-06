package com.fooddelivery.food.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fooddelivery.food.dto.ErrorResponse;
import com.fooddelivery.food.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse errorResponse = new ErrorResponse(
                                    HttpServletResponse.SC_UNAUTHORIZED,
                                    "UNAUTHORIZED",
                                    "Full authentication is required to access this resource",
                                    request.getRequestURI()
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            ErrorResponse errorResponse = new ErrorResponse(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "FORBIDDEN",
                                    "Access is denied",
                                    request.getRequestURI()
                            );
                            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/restaurants", "/restaurants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/foods", "/foods/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/restaurants", "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/restaurants", "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/restaurants", "/restaurants/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/foods", "/foods/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/foods", "/foods/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/foods", "/foods/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

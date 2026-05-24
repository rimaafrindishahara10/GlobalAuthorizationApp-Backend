package com.dishahara.config;

import com.dishahara.dtos.ApiError;
import com.dishahara.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Objects;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth->
                        auth
                                .requestMatchers("/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/refreshToken","/api/v1/auth/logout").permitAll()
                                .anyRequest().authenticated()
                )
                .exceptionHandling(e->e.authenticationEntryPoint((request, response, e1)->{

                    //e1.printStackTrace();
                    response.setStatus(401);
                    response.setContentType("application/json;charset=utf-8");
                    String message = "Unauthorized Access ! " + e1.getMessage();
                    String error = request.getAttribute("error").toString();
                    if (error != null) {
                        message = error;
                    }
                   // Map<String, Object> errorMap = Map.of("message",message ,"StatusCode",401);
                    ApiError apiError = ApiError.of(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", message, request.getRequestURI());
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(apiError));
                        }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);


        return http.build();
    }

}

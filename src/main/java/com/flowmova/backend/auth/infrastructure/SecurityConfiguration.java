package com.flowmova.backend.auth.infrastructure;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .formLogin(formLogin -> formLogin.disable())
                .httpBasic(httpBasic -> httpBasic.disable())
                .exceptionHandling(exceptionHandling ->
                        exceptionHandling.authenticationEntryPoint(restAuthenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}/catalog-categories").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}/catalogs").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}/service-units").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/companies/{companyId}/service-units/{serviceUnitId}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/service-units/{serviceUnitId}/tickets").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tickets/guest-access").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/guest-access/cancel").permitAll()
                        .requestMatchers(HttpMethod.PATCH, "/api/tickets/guest-access/confirm-treatment").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/public/locations/{publicAccessSlug}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException(username);
        };
    }
}

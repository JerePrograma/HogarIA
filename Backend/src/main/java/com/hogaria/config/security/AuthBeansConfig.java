package com.hogaria.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthBeansConfig {

    /**
     * Para que PasswordEncoder este disponible para inyectar en AuthServiceImpl (y en cualquier
     * otro componente que lo necesite, por ejemplo al guardar usuarios).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Expone el AuthenticationManager que construye Spring a partir de los AuthenticationProvider
     * (incluido el DaoAuthenticationProvider que registra tu UserDetailsService + PasswordEncoder).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}

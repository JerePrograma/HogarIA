package com.hogaria.config.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.*;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;

import java.security.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // == CARGA DEL KEYSTORE RSA ==
    @Value("${jwt.keystore-location}")
    private Resource keystore;
    @Value("${jwt.keystore-password}")
    private char[] keystorePass;
    @Value("${jwt.key-alias}")
    private String keyAlias;
    @Value("${jwt.key-password}")
    private char[] keyPass;

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keystore.getInputStream(), keystorePass);

        PrivateKey pk = (PrivateKey) ks.getKey(keyAlias, keyPass);
        Certificate cert = ks.getCertificate(keyAlias);
        PublicKey pub = cert.getPublicKey();

        RSAKey rsaJwk = new RSAKey.Builder((RSAPublicKey) pub)
                .privateKey((RSAPrivateKey) pk)
                .keyID(keyAlias)
                .build();

        return new ImmutableJWKSet<>(new JWKSet(rsaJwk));
    }

    // == ENCODER: firma con RS256 ==
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    // == DECODER: verifica con la misma clave publica ==
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) throws JOSEException {
        // obtenemos la JWKSet y sacamos la RSAKey
        ImmutableJWKSet<SecurityContext> jkws = (ImmutableJWKSet<SecurityContext>) jwkSource;
        List<JWK> keys = jkws.getJWKSet().getKeys();
        RSAKey rsaKey = (RSAKey) keys.getFirst();
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }

    // == SECURITY FILTER CHAIN ==
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtDecoder jwtDecoder) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**", "/swagger-ui/**", "/v1/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                );
        return http.build();
    }
}

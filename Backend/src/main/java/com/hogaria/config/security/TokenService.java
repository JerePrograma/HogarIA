package com.hogaria.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.hogaria.auth.model.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Service for generating and validating JWT tokens using RSA keys from a JKS keystore.
 */
@Service
public class TokenService {

    @Value("${jwt.keystore-location}")
    private Resource keystoreLocation;

    @Value("${jwt.keystore-password}")
    private String keystorePassword;

    @Value("${jwt.key-alias}")
    private String keyAlias;

    @Value("${jwt.key-password}")
    private String keyPassword;

    @Value("${jwt.issuer}")
    private String issuer;

    private KeyStore keyStore;

    @PostConstruct
    public void init() throws Exception {
        keyStore = KeyStore.getInstance("JKS");
        try (InputStream is = keystoreLocation.getInputStream()) {
            keyStore.load(is, keystorePassword.toCharArray());
        }
    }

    private Algorithm getAlgorithm() throws Exception {
        // Carga la clave privada para firmar y la publica para verificar
        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
        PublicKey publicKey = keyStore.getCertificate(keyAlias).getPublicKey();
        return Algorithm.RSA256(
                (RSAPublicKey) publicKey,
                (RSAPrivateKey) privateKey
        );
    }

    public String generateAccessToken(User user) throws Exception {
        Algorithm alg = getAlgorithm();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withClaim("id", user.getId())
                .withClaim("role", user.getRole())
                .withClaim("type", "ACCESS")
                .withExpiresAt(expiration(2))
                .sign(alg);
    }

    public String generateRefreshToken(User user) throws Exception {
        Algorithm alg = getAlgorithm();
        return JWT.create()
                .withIssuer(issuer)
                .withSubject(user.getUsername())
                .withClaim("id", user.getId())
                .withClaim("role", user.getRole())
                .withClaim("type", "REFRESH")
                .withExpiresAt(expiration(168))
                .sign(alg);
    }

    public DecodedJWT verifyToken(String token) throws Exception {
        Algorithm alg = getAlgorithm();
        return JWT.require(alg)
                .withIssuer(issuer)
                .build()
                .verify(token);
    }

    public String getSubject(String token) throws Exception {
        return verifyToken(token).getSubject();
    }

    public boolean isAccessToken(String token) throws Exception {
        return "ACCESS".equals(verifyToken(token).getClaim("type").asString());
    }

    private Instant expiration(int hours) {
        return LocalDateTime.now()
                .plusHours(hours)
                .toInstant(ZoneOffset.of("-03:00"));
    }
}

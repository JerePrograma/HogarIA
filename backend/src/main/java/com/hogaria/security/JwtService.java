package com.hogaria.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
  private final ObjectMapper objectMapper;
  private final byte[] secret;
  private final long expirationSeconds;

  public JwtService(ObjectMapper objectMapper, @Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expiration-seconds:43200}") long expirationSeconds) {
    this.objectMapper = objectMapper;
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.expirationSeconds = expirationSeconds;
  }

  public String generateToken(UUID userId, String email) {
    try {
      long now = Instant.now().getEpochSecond();
      String header = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
      String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(Map.of("sub", userId.toString(), "email", email, "iat", now, "exp", now + expirationSeconds)));
      String signature = URL_ENCODER.encodeToString(hmacSha256(header + "." + payload));
      return header + "." + payload + "." + signature;
    } catch (Exception e) {
      throw new IllegalStateException("Could not generate JWT token", e);
    }
  }

  public UUID parseUserId(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) return null;
      byte[] expectedSig = hmacSha256(parts[0] + "." + parts[1]);
      byte[] providedSig = URL_DECODER.decode(parts[2]);
      if (!java.security.MessageDigest.isEqual(expectedSig, providedSig)) return null;
      Map<String, Object> claims = objectMapper.readValue(URL_DECODER.decode(parts[1]), new TypeReference<>() {});
      Number exp = (Number) claims.get("exp");
      if (exp == null || Instant.now().getEpochSecond() >= exp.longValue()) return null;
      return UUID.fromString((String) claims.get("sub"));
    } catch (Exception e) {
      return null;
    }
  }

  private byte[] hmacSha256(String value) throws Exception {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret, "HmacSHA256"));
    return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
  }
}

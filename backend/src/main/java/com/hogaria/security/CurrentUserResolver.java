package com.hogaria.security;

import com.hogaria.exception.BadRequestException;
import com.hogaria.exception.ForbiddenException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {
  @Value("${app.security.allow-x-user-id-fallback:false}")
  private boolean allowXUserIdFallback;

  public UUID parse(String id) {
    try {
      return currentUserId();
    } catch (ForbiddenException ex) {
      if (allowXUserIdFallback && id != null) {
        try { return UUID.fromString(id); } catch (Exception e) { throw new BadRequestException("Invalid X-User-Id"); }
      }
      throw ex;
    }
  }

  public UUID currentUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal() == null) throw new ForbiddenException("Authentication required");
    Object principal = auth.getPrincipal();
    if (principal instanceof UUID id) return id;
    try { return UUID.fromString(principal.toString()); } catch (Exception e) { throw new ForbiddenException("Authentication required"); }
  }
}

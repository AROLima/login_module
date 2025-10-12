package com.login.login.web;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/** Utilitário de cookies HttpOnly com SameSite via header Set-Cookie. */
public class CookieUtils {
  public static Cookie build(String name, String value, int maxAgeSeconds, String domain, boolean secure, String sameSite) {
    var c = new Cookie(name, value);
    c.setHttpOnly(true);
    c.setPath("/");
    c.setMaxAge(maxAgeSeconds);
    if (domain!=null && !domain.isBlank()) c.setDomain(domain);
    c.setSecure(secure);
    return c;
  }
  public static void addWithSameSite(HttpServletResponse res, Cookie cookie, String sameSite) {
    String header = "%s=%s; Path=%s; Max-Age=%d; %s%s%s".formatted(
      cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getMaxAge(),
      cookie.getSecure() ? "Secure; " : "", "HttpOnly; ", (sameSite!=null ? "SameSite="+sameSite : "")
    );
    if (cookie.getDomain()!=null) header += "; Domain="+cookie.getDomain();
    res.addHeader("Set-Cookie", header);
  }

  
}
package com.login.login.config;

import org.springframework.web.filter.CorsFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Se o front estiver no mesmo host/porta, isso pode ser removido.
 * Mantido aqui como exemplo; ajusta a origem conforme necessário.
 */
@Configuration
public class CorsConfig {
  @Bean 
  public CorsFilter corsFilter() {
    var source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.addAllowedOriginPattern("http://localhost:8080");
    cfg.addAllowedHeader("*");
    cfg.addAllowedMethod("*");
    cfg.setAllowCredentials(true);
    source.registerCorsConfiguration("/**", cfg);
    return new CorsFilter(source);
  }
}
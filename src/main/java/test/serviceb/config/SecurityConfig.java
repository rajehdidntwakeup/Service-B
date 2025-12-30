package test.serviceb.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * The SecurityConfig class is a configuration class that sets up Spring Security
 * features such as authentication, authorization, CSRF, CORS, and other related
 * security settings for the application.
 * This class utilizes Spring's Java Config support and defines beans for configuring
 * the application's security filter chain and CORS settings.
 */
@Configuration
public class SecurityConfig {

  /**
   * Configures the security filter chain for the application, setting up CSRF, CORS,
   * and authorization rules.
   *
   * @param http the {@link HttpSecurity} object used to configure security for the application
   * @return the configured {@link SecurityFilterChain} to be used by the application
   * @throws Exception if an error occurs while building the security configuration
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/order/**", "/h2-console/**").permitAll()
            .anyRequest().permitAll()
        );

    // For H2 console frames if you use it
    http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));

    return http.build();
  }

  /**
   * Configures and provides a {@link CorsConfigurationSource} for handling Cross-Origin Resource Sharing (CORS)
   * settings in the application. This method sets up CORS policies such as allowed origin patterns, HTTP methods,
   * headers, exposed headers, credentials support, and cache duration.
   *
   * @return a {@link CorsConfigurationSource} instance configured with CORS policies for the application
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOriginPatterns(List.of("*"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("Authorization", "Link"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

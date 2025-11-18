package test.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * The ServiceBApplication class is the main application class for the Service-B.
 */
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
@SpringBootApplication
public class ServiceBApplication {

  /**
   * The main method serves as the entry point for the Service-B application.
   * It initializes and runs the Spring Boot application context for ServiceBApplication.
   *
   * @param args command-line arguments passed to the application.
   */
  public static void main(String[] args) {
    SpringApplication.run(ServiceBApplication.class, args);
  }

  /**
   * Configures Cross-Origin Resource Sharing (CORS) for the application.
   *
   * @return a {@link WebMvcConfigurer} bean that customizes CORS settings, including allowed origins, methods, headers, and credentials.
   */
  @Bean
  public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins("http://localhost:8080")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Authorization", "Link")
            .allowCredentials(true)
            .maxAge(3600);
      }
    };
  }
}

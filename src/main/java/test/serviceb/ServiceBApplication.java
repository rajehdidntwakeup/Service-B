package test.serviceb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

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

}

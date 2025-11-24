package test.serviceb;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ServiceBApplicationTests {

  /**
   * Tests whether the application context loads successfully.
   * This method ensures that the Spring application context
   * is properly initialized without any issues.
   */
  @Test
  void contextLoads() {
  }

}

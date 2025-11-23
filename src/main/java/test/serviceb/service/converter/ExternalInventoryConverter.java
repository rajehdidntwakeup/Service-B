package test.serviceb.service.converter;

import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import test.serviceb.domain.dto.ExternalInventory;


/**
 * A Spring Converter implementation for converting a String input into an
 * instance of {@link ExternalInventory}.
 * This class is annotated with {@code @Component} to allow Spring to detect
 * and manage it within the application context. Additionally, it is annotated
 * with {@code @ConfigurationPropertiesBinding} to support Spring's configuration
 * properties binding mechanism.
 * The expected format for the input String is a comma-separated value containing
 * the name and URL of an external inventory, for example: "name,url".
 * If the input String does not adhere to the expected format, an
 * {@code IllegalArgumentException} is thrown.
 */
@Component
@ConfigurationPropertiesBinding
public class ExternalInventoryConverter implements Converter<String, ExternalInventory> {

  private static final String DELIMITER = ",";
  private static final int EXPECTED_PARTS = 2;

  /**
   * Converts a comma-separated string representation of an external inventory into an
   * {@link ExternalInventory} object. The input string is expected to contain two
   * components: the name and the URL of the external inventory, separated by a comma.
   * If the input string does not conform to the expected format, an
   * {@link IllegalArgumentException} is thrown.
   *
   * @param source the comma-separated string representation of the external inventory.
   *               For example, a valid input might look like "name,url".
   * @return an {@link ExternalInventory} object constructed from the input string.
   * @throws IllegalArgumentException if the input string does not contain exactly two
   *                                  comma-separated components.
   */
  @Override
  public ExternalInventory convert(String source) {
    String[] data = source.split(DELIMITER);
    if (data.length == EXPECTED_PARTS) {
      return new ExternalInventory(data[0], data[1]);
    }
    throw new IllegalArgumentException("Invalid external service format: " + source);
  }
}

package test.serviceb.service.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import test.serviceb.domain.dto.ExternalInventory;

/**
 * This class represents a configuration properties component for managing
 * external inventory settings in the application. It is annotated with
 * {@code @Component} and {@code @ConfigurationProperties} to integrate
 * with Spring's configuration system.
 * Configuration for external inventory is defined under the prefix
 * "external.inventory" and uses a map structure to store mappings between
 * external inventory identifiers and their respective {@code ExternalInventory}
 * objects.
 */
@Component
@ConfigurationProperties(prefix = "external.inventory")
public class ConversionProperties {

  /**
   * A map representing configurations or mappings for external inventory systems.
   * The keys in this map represent unique identifiers for external inventory sources,
   * while the values are instances of {@link ExternalInventory}, containing details
   * such as the name and URL of the corresponding external inventory.
   * This variable is used to support integration with multiple external inventory
   * systems by providing a configuration-driven approach.
   */
  private Map<String, ExternalInventory> externalInventory = new HashMap<>();


  /**
   * Retrieves the map of external inventory configurations.
   * The map contains external inventory identifiers as keys and their corresponding
   * {@link ExternalInventory} objects as values, which include details such as the name
   * and URL of the external inventory source.
   *
   * @return a map where keys are strings representing unique identifiers for external inventory sources, and values are {@link ExternalInventory} objects.
   */
  public Map<String, ExternalInventory> getExternalInventory() {
    return externalInventory;
  }

  /**
   * Sets the external inventory configuration for the application.
   * This method is intended to populate the external inventory map with
   * ExternalInventory objects mapped to their respective keys.
   *
   * @param externalInventory a map where the key is a string identifier
   *                          and the value is an ExternalInventory object
   */
  public void setExternalInventory(Map<String, ExternalInventory> externalInventory) {
    this.externalInventory = externalInventory;
  }
}

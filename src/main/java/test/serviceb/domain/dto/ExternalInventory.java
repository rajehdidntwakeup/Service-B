package test.serviceb.domain.dto;

/**
 * Represents an external inventory source with a name and a URL.
 * This class is used to encapsulate information about external inventory
 * systems or providers.
 */
public class ExternalInventory {
  private String name;
  private String url;


  /**
   * Constructs an instance of ExternalInventory with the specified name and URL.
   *
   * @param name the name of the external inventory source or provider
   * @param url  the URL associated with the external inventory source or provider
   */
  public ExternalInventory(String name, String url) {
    this.name = name;
    this.url = url;
  }

  /**
   * Retrieves the name of the external inventory source.
   *
   * @return the name of the inventory source as a string
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name of the external inventory source.
   *
   * @param name the name to set for the external inventory source
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieves the URL associated with this external inventory source.
   *
   * @return the URL of the external inventory as a string
   */
  public String getUrl() {
    return url;
  }

  /**
   * Sets the URL of the external inventory source.
   *
   * @param url the URL to set for the external inventory source
   */
  public void setUrl(String url) {
    this.url = url;
  }
}

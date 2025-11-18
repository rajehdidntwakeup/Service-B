package test.serviceb.domain.dto;

/**
 * Represents a Data Transfer Object (DTO) for an inventory item.
 * This class is used to encapsulate details about an inventory item, including its name,
 * stock quantity, price, and description.
 */
public class InventoryItemDto {
  private String name;
  private int stock;
  private double price;
  private String description;

  /**
   * Default constructor for the InventoryItemDto class.
   * Initializes an instance of InventoryItemDto with default values for its properties.
   */
  public InventoryItemDto() {
  }

  /**
   * Constructs an instance of InventoryItemDto with the specified name, stock, price, and description.
   *
   * @param name        the name of the inventory item
   * @param stock       the quantity of the inventory item in stock
   * @param price       the price of the inventory item
   * @param description the description of the inventory item
   */
  public InventoryItemDto(String name, int stock, double price, String description) {
    this.name = name;
    this.stock = stock;
    this.price = price;
    this.description = description;
  }

  /**
   * Retrieves the name of the inventory item.
   *
   * @return the name of the inventory item as a String
   */
  public String getName() {
    return name;
  }

  /**
   * Updates the name of the inventory item.
   *
   * @param name the new name to set for the inventory item
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieves the stock quantity of the inventory item.
   *
   * @return the stock quantity as an integer
   */
  public int getStock() {
    return stock;
  }

  /**
   * Sets the stock level for the inventory item.
   *
   * @param stock the stock level to set; must be a non-negative integer
   */
  public void setStock(int stock) {
    this.stock = stock;
  }

  /**
   * Retrieves the price of the inventory item.
   *
   * @return the price of the item as a double
   */
  public double getPrice() {
    return price;
  }

  /**
   * Sets the price of the inventory item.
   *
   * @param price the price to set, represented as a double
   */
  public void setPrice(double price) {
    this.price = price;
  }

  /**
   * Retrieves the description of the inventory item.
   *
   * @return the description of the item as a string
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the inventory item.
   *
   * @param description the description of the inventory item
   */
  public void setDescription(String description) {
    this.description = description;
  }
}

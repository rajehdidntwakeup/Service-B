package test.serviceb.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a Data Transfer Object (DTO) for an item in an order.
 * This class is used to encapsulate information about a single order item,
 * including its unique identifier, price, and quantity.
 */
public class OrderItemDto {

  @Min(value = 0, message = "Item ID must be non-negative")
  private int itemId;
  @NotBlank(message = "Item name must be provided")
  private String itemName;
  @Min(value = 0, message = "Price must be non-negative")
  private double price;
  @Min(value = 1, message = "Quantity must be greater than zero")
  private int quantity;

  /**
   * Default constructor for the OrderItemDto class.
   * Initializes an instance of OrderItemDto with default values for its properties.
   */
  public OrderItemDto() {
  }

  /**
   * Constructs an instance of OrderItemDto with the specified item ID, item name, price, and quantity.
   *
   * @param itemId   the unique identifier for the order item; must be non-negative
   * @param itemName the name of the order item; must not be blank
   * @param price    the price of the order item; must be non-negative
   * @param quantity the quantity of the order item; must be greater than zero
   */
  public OrderItemDto(int itemId, String itemName, double price, int quantity) {
    this.itemId = itemId;
    this.itemName = itemName;
    this.price = price;
    this.quantity = quantity;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }
}

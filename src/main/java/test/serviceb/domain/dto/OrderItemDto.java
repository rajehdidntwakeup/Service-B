package test.serviceb.domain.dto;

import jakarta.validation.constraints.Min;

/**
 * Represents a Data Transfer Object (DTO) for an item in an order.
 * This class is used to encapsulate information about a single order item,
 * including its unique identifier, price, and quantity.
 */
public class OrderItemDto {

  @Min(value = 0, message = "Item ID must be non-negative")
  private int itemId;
  @Min(value = 0, message = "Price must be non-negative")
  private double price;
  @Min(value = 1, message = "Quantity must be greater than zero")
  private int quantity;

  public OrderItemDto() {
  }

  public OrderItemDto(int itemId, double price, int quantity) {
    this.itemId = itemId;
    this.price = price;
    this.quantity = quantity;
  }

  public int getItemId() {
    return itemId;
  }

  public void setItemId(int itemId) {
    this.itemId = itemId;
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

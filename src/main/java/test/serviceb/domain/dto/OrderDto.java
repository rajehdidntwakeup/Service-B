package test.serviceb.domain.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Represents a Data Transfer Object (DTO) for an order.
 * This class is used to encapsulate information about an order,
 * including its total price, status, and associated order items.
 */
public class OrderDto {

  @Min(value = 0, message = "Total price must be non-negative")
  private double totalPrice;
  @NotBlank(message = "Status must be provided")
  private String status;
  private List<OrderItemDto> items = new ArrayList<>();

  /**
   * Default constructor for the OrderDto class.
   * Initializes an instance of OrderDto with default values for its properties.
   */
  public OrderDto() {
  }

  /**
   * Constructs an instance of OrderDto with the specified total price, status,
   * and list of order items.
   *
   * @param totalPrice the total price of the order; must be non-negative
   * @param status     the status of the order; must not be blank
   * @param items      the list of order items; may be an empty list
   */
  public OrderDto(double totalPrice, String status, List<OrderItemDto> items) {
    this.totalPrice = totalPrice;
    this.status = status;
    this.items = items;
  }

  /**
   * Retrieves the total price of the order.
   *
   * @return the total price of the order as a double
   */
  public double getTotalPrice() {
    return totalPrice;
  }

  /**
   * Sets the total price of the order.
   *
   * @param totalPrice the total price of the order, must be non-negative
   */
  public void setTotalPrice(double totalPrice) {
    this.totalPrice = totalPrice;
  }

  /**
   * Retrieves the current status of the order.
   *
   * @return the status of the order as a non-blank string
   */
  public String getStatus() {
    return status;
  }

  /**
   * Sets the status of the order.
   *
   * @param status the status of the order, must not be blank
   */
  public void setStatus(String status) {
    this.status = status;
  }

  /**
   * Retrieves the list of order items associated with this order.
   *
   * @return a list of OrderItemDto objects representing the items in the order.
   */
  public List<OrderItemDto> getItems() {
    return items;
  }

  /**
   * Sets the list of order items.
   *
   * @param items the list of order items to set, represented as a List of OrderItemDto objects
   */
  public void setItems(List<OrderItemDto> items) {
    this.items = items;
  }
}

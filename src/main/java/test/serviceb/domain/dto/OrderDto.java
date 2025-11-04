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

  public OrderDto() {
  }

  public OrderDto(double totalPrice, String status, List<OrderItemDto> items) {
    this.totalPrice = totalPrice;
    this.status = status;
    this.items = items;
  }

  public double getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(double totalPrice) {
    this.totalPrice = totalPrice;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public List<OrderItemDto> getItems() {
    return items;
  }

  public void setItems(List<OrderItemDto> items) {
    this.items = items;
  }
}

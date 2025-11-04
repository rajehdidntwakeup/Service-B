package test.serviceb.domain.dto;

public class InventoryItemDto {
  private String name;
  private int stock;
  private double price;
  private String description;

  public InventoryItemDto() {
  }

  public InventoryItemDto(String name, int stock, double price, String description) {
    this.name = name;
    this.stock = stock;
    this.price = price;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getStock() {
    return stock;
  }

  public void setStock(int stock) {
    this.stock = stock;
  }

  public double getPrice() {
    return price;
  }

  public void setPrice(double price) {
    this.price = price;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}

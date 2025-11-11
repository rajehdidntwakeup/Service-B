package test.serviceb.domain;

/**
 * Represents a general item with specific attributes such as ID, name, stock quantity, price, and description.
 * This class can be used in scenarios where item details are required,
 * such as inventory management or order processing systems.
 */
public class Item {
  private int id;
  private String name;
  private int stock;
  private double price;
  private String description;

  /**
   * Default constructor for the Item class.
   * Initializes a new instance of the Item entity with default values.
   */
  public Item() {
  }

  /**
   * Constructs an instance of the Item class with the specified parameters.
   *
   * @param id          the unique identifier for the item
   * @param name        the name of the item
   * @param stock       the quantity of the item available in stock
   * @param price       the price of the item
   * @param description a brief description of the item
   */
  public Item(int id, String name, int stock, double price, String description) {
    this.id = id;
    this.name = name;
    this.stock = stock;
    this.price = price;
    this.description = description;
  }

  /**
   * Retrieves the identifier of the item.
   *
   * @return the identifier of the item as an integer
   */
  public int getId() {
    return id;
  }

  /**
   * Sets the unique identifier for the item.
   *
   * @param id the identifier to set, represented as an integer
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * Retrieves the name of the item.
   *
   * @return the name of the item as a String
   */
  public String getName() {
    return name;
  }

  /**
   * Updates the name of the item.
   *
   * @param name the new name to be assigned to the item
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Retrieves the current stock quantity for an item.
   *
   * @return the stock quantity as an integer
   */
  public int getStock() {
    return stock;
  }

  /**
   * Updates the stock quantity for the item.
   *
   * @param stock the new stock quantity to be set for the item
   */
  public void setStock(int stock) {
    this.stock = stock;
  }

  /**
   * Retrieves the price of the item.
   *
   * @return the price of the item as a double
   */
  public double getPrice() {
    return price;
  }

  /**
   * Updates the price of the item.
   *
   * @param price The new price to be set for the item, represented as a double.
   *              This should be a non-negative value.
   */
  public void setPrice(double price) {
    this.price = price;
  }

  /**
   * Retrieves the description of the item.
   *
   * @return the description of the item as a String
   */
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description for this item.
   *
   * @param description the description to set for the item as a string
   */
  public void setDescription(String description) {
    this.description = description;
  }
}

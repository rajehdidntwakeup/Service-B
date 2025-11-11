package test.serviceb.service;

import java.util.List;

import test.serviceb.domain.Orders;
import test.serviceb.domain.dto.OrderDto;

/**
 * The OrderService interface defines the contract for managing orders.
 */
public interface OrderService {

  /**
   * Creates a new order based on the provided order data transfer object (DTO).
   * This method initializes and persists an order entity using the information
   * encapsulated in the given {@code OrderDto}.
   *
   * @param order an instance of {@code OrderDto} containing the necessary details
   *              for the order, such as total price, status, and associated items.
   * @return an instance of {@code Orders} representing the newly created order.
   */
  Orders createOrder(OrderDto order);

  /**
   * Retrieves the order associated with the specified order ID.
   *
   * @param orderId the unique identifier of the order to be retrieved.
   * @return the {@code Orders} object corresponding to the specified order ID, or {@code null} if no order with the given ID is found.
   */
  Orders getOrder(int orderId);

  /**
   * Retrieves a list of all orders.
   *
   * @return a list of {@code Orders} objects, each representing an order in the system.
   */
  List<Orders> getAllOrders();

  /**
   * Updates an existing order with the provided details.
   *
   * @param orderId the unique identifier of the order to update.
   * @param order   the new details for the order, encapsulated in an OrderDto object.
   * @return the updated order entity as an Orders object.
   */
  Orders updateOrder(int orderId, OrderDto order);
}

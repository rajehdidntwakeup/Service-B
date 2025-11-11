package test.serviceb.controller;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import test.serviceb.domain.Orders;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.service.OrderService;

/**
 * The OrderController class is a REST controller responsible for managing orders.
 * It provides endpoints for creating, retrieving, and updating orders.
 */
@RestController()
@RequestMapping("/api/order")
public class OrderController {

  private final OrderService orderService;

  /**
   * Constructs a new OrderController with the specified OrderService dependency.
   *
   * @param orderService the OrderService to be used by this controller, responsible for handling
   *                     business logic related to orders
   */
  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  /**
   * Creates a new order based on the provided order details.
   *
   * @param orderDto the data transfer object containing the details of the order to be created
   * @return a {@link ResponseEntity} containing the created {@link Orders} object with a status of {@code HttpStatus.CREATED}
   */
  @PostMapping
  public ResponseEntity<Orders> createOrder(@Valid @RequestBody OrderDto orderDto) {
    Orders order = orderService.createOrder(orderDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  /**
   * Retrieves a list of all orders.
   *
   * @return a ResponseEntity containing a list of {@code Orders} objects.
   */
  @GetMapping
  public ResponseEntity<List<Orders>> getAllOrders() {
    List<Orders> orders = orderService.getAllOrders();
    return ResponseEntity.ok(orders);
  }

  /**
   * Retrieves the details of an order based on the provided order ID.
   *
   * @param id the unique identifier of the order to be retrieved
   * @return a ResponseEntity containing the order details if found, or a not found HTTP status if the order does not exist
   */
  @GetMapping("/{id}")
  public ResponseEntity<Orders> getOrder(@PathVariable int id) {
    Orders order = orderService.getOrder(id);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(order);
  }

  /**
   * Updates an existing order with new information provided in the request body.
   * If the order with the specified ID does not exist, a 404 Not Found response is returned.
   *
   * @param id       the unique identifier of the order to be updated.
   * @param orderDto the data transfer object containing updated details for the order.
   * @return a ResponseEntity containing the updated order object and a 200 OK status if successful, or a 404 Not Found status if the order does not exist.
   */
  @PutMapping("/{id}")
  public ResponseEntity<Orders> updateOrder(@PathVariable int id, @Valid @RequestBody OrderDto orderDto) {
    Orders order = orderService.updateOrder(id, orderDto);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(order);
  }
}

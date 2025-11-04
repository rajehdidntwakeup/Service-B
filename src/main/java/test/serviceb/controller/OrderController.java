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

@RestController()
@RequestMapping("/api/order")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  public ResponseEntity<Orders> createOrder(@Valid @RequestBody OrderDto orderDto) {
    Orders order = orderService.createOrder(orderDto);
    return ResponseEntity.status(HttpStatus.CREATED).body(order);
  }

  @GetMapping
  public ResponseEntity<List<Orders>> getAllOrders() {
    List<Orders> orders = orderService.getAllOrders();
    return ResponseEntity.ok(orders);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Orders> getOrder(@PathVariable int id) {
    Orders order = orderService.getOrder(id);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(order);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Orders> updateOrder(@PathVariable int id, @Valid @RequestBody OrderDto orderDto) {
    Orders order = orderService.updateOrder(id, orderDto);
    if (order == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(order);
  }
}

package test.serviceb.controller.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import test.serviceb.controller.OrderController;
import test.serviceb.domain.Orders;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.service.OrderService;

/**
 * Edge-case unit tests for {@link OrderController} (pure unit, no Spring context).
 */
public class OrderControllerEdgeCaseUnitTest {

  @Mock
  private OrderService orderService;

  private OrderController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new OrderController(orderService);
  }

  @Test
  @DisplayName("getAllOrders should return 200 OK with empty list when service returns empty")
  void getAllOrders_shouldReturnEmptyList() {
    when(orderService.getAllOrders()).thenReturn(Collections.emptyList());

    ResponseEntity<List<Orders>> response = controller.getAllOrders();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(0, response.getBody().size());
    verify(orderService).getAllOrders();
  }

  @Test
  @DisplayName("getOrder should return 404 for negative id when service returns null")
  void getOrder_shouldReturnNotFoundForNegativeId() {
    int id = -1;
    when(orderService.getOrder(id)).thenReturn(null);

    ResponseEntity<Orders> response = controller.getOrder(id);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(orderService).getOrder(id);
  }

  @Test
  @DisplayName("getOrder should return 404 for Integer.MAX_VALUE when service returns null")
  void getOrder_shouldReturnNotFoundForMaxInt() {
    int id = Integer.MAX_VALUE;
    when(orderService.getOrder(id)).thenReturn(null);

    ResponseEntity<Orders> response = controller.getOrder(id);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(orderService).getOrder(id);
  }

  @Test
  @DisplayName("createOrder should return 201 with null body if service returns null (documents current behavior)")
  void createOrder_shouldReturnCreatedWithNullBodyWhenServiceReturnsNull() {
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(0.01);
    dto.setStatus("CONFIRMED");
    dto.setItems(Collections.emptyList());

    when(orderService.createOrder(any(OrderDto.class))).thenReturn(null);

    ResponseEntity<Orders> response = controller.createOrder(dto);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    // Body is null because service returned null; this test documents current controller behavior
    assertEquals(null, response.getBody());
    verify(orderService).createOrder(dto);
  }

  @Test
  @DisplayName("updateOrder should propagate runtime exceptions from service")
  void updateOrder_shouldPropagateExceptions() {
    int id = -42; // unusual id value
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(1.0);
    dto.setStatus("SHIPPED");
    dto.setItems(Collections.emptyList());

    when(orderService.updateOrder(eq(id), any(OrderDto.class)))
        .thenThrow(new RuntimeException("Service failure"));

    assertThrows(RuntimeException.class, () -> controller.updateOrder(id, dto));
    verify(orderService).updateOrder(id, dto);
  }
}

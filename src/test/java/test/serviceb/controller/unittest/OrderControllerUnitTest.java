package test.serviceb.controller.unittest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import test.serviceb.controller.OrderController;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.service.OrderService;

/**
 * Unit tests for {@link OrderController} focusing on controller logic only.
 * The underlying {@link OrderService} is mocked.
 */
public class OrderControllerUnitTest {

  @Mock
  private OrderService orderService;

  private OrderController controller;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new OrderController(orderService);
  }

  @Test
  @DisplayName("createOrder should return 201 Created with body from service and pass through the DTO")
  void createOrder_shouldReturnCreatedAndPassDto() {
    OrderDto dto = new OrderDto();
    Orders saved = new Orders(100.0, Status.CONFIRMED, Collections.emptyList());

    when(orderService.createOrder(any(OrderDto.class))).thenReturn(saved);

    ResponseEntity<Orders> response = controller.createOrder(dto);

    assertNotNull(response);
    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(saved, response.getBody());

    ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
    verify(orderService).createOrder(captor.capture());
    assertEquals(dto, captor.getValue());
  }

  @Test
  @DisplayName("getAllOrders should return 200 OK with list from service")
  void getAllOrders_shouldReturnOk() {
    Orders o1 = new Orders(10.0, Status.CONFIRMED, Collections.emptyList());
    Orders o2 = new Orders(20.0, Status.SHIPPED, Collections.emptyList());
    List<Orders> list = Arrays.asList(o1, o2);
    when(orderService.getAllOrders()).thenReturn(list);

    ResponseEntity<List<Orders>> response = controller.getAllOrders();

    assertNotNull(response);
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(list, response.getBody());
    verify(orderService).getAllOrders();
  }

  @Test
  @DisplayName("getOrder should return 200 OK with order when found")
  void getOrder_shouldReturnOkWhenFound() {
    int id = 1;
    Orders order = new Orders(42.0, Status.CANCELLED, Collections.emptyList());
    when(orderService.getOrder(id)).thenReturn(order);

    ResponseEntity<Orders> response = controller.getOrder(id);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(order, response.getBody());
    verify(orderService).getOrder(id);
  }

  @Test
  @DisplayName("getOrder should return 404 Not Found when service returns null")
  void getOrder_shouldReturnNotFoundWhenNull() {
    int id = 999;
    when(orderService.getOrder(id)).thenReturn(null);

    ResponseEntity<Orders> response = controller.getOrder(id);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(orderService).getOrder(id);
  }

  @Test
  @DisplayName("updateOrder should return 200 OK with updated order when found and pass id and DTO")
  void updateOrder_shouldReturnOkWhenFoundAndPassArgs() {
    int id = 5;
    OrderDto dto = new OrderDto();
    Orders updated = new Orders(77.0, Status.SHIPPED, Collections.emptyList());
    when(orderService.updateOrder(eq(id), any(OrderDto.class))).thenReturn(updated);

    ResponseEntity<Orders> response = controller.updateOrder(id, dto);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(updated, response.getBody());

    ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
    verify(orderService).updateOrder(eq(id), captor.capture());
    assertEquals(dto, captor.getValue());
  }

  @Test
  @DisplayName("updateOrder should return 404 Not Found when service returns null")
  void updateOrder_shouldReturnNotFoundWhenNull() {
    int id = 7;
    OrderDto dto = new OrderDto();
    when(orderService.updateOrder(eq(id), any(OrderDto.class))).thenReturn(null);

    ResponseEntity<Orders> response = controller.updateOrder(id, dto);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    verify(orderService).updateOrder(eq(id), any(OrderDto.class));
  }
}

package test.serviceb.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.service.OrderService;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb-controller-get-order-integration-test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GetOrderIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private OrderService orderService;

  @Test
  @DisplayName("GET /api/order should return 200 OK and a list of orders")
  void shouldReturnAllOrders_Return200AndList() throws Exception {
    // Given
    Orders o1 = new Orders();
    o1.setTotalPrice(25.5);
    o1.setStatus(Status.CONFIRMED);
    o1.addOrderItem(new OrderItem(10, "alpha-widget", 2, 5.25));

    Orders o2 = new Orders();
    o2.setTotalPrice(40.0);
    o2.setStatus(Status.SHIPPED);
    o2.addOrderItem(new OrderItem(11, "beta-gadget", 4, 10.0));

    given(orderService.getAllOrders()).willReturn(List.of(o1, o2));

    // When & Then
    mockMvc.perform(get("/api/order"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].totalPrice").value(25.5))
        .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
        .andExpect(jsonPath("$[0].orderItems[0].itemId").value(10))
        .andExpect(jsonPath("$[0].orderItems[0].itemName").value("alpha-widget"))
        .andExpect(jsonPath("$[0].orderItems[0].quantity").value(2))
        .andExpect(jsonPath("$[0].orderItems[0].price").value(5.25))
        .andExpect(jsonPath("$[1].totalPrice").value(40.0))
        .andExpect(jsonPath("$[1].status").value("SHIPPED"))
        .andExpect(jsonPath("$[1].orderItems[0].itemId").value(11))
        .andExpect(jsonPath("$[1].orderItems[0].itemName").value("beta-gadget"))
        .andExpect(jsonPath("$[1].orderItems[0].quantity").value(4))
        .andExpect(jsonPath("$[1].orderItems[0].price").value(10.0));
  }

  @Test
  @DisplayName("GET /api/order should include orders that have an empty orderItems array")
  void shouldReturnOrderWithEmptyItemsArray() throws Exception {
    // Given
    Orders o1 = new Orders();
    o1.setTotalPrice(0.0);
    o1.setStatus(Status.CONFIRMED);
    // No items added -> expect empty array in JSON

    given(orderService.getAllOrders()).willReturn(List.of(o1));

    // When & Then
    mockMvc.perform(get("/api/order"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].orderItems").isArray())
        .andExpect(jsonPath("$[0].orderItems.length()")
            .value(0));
  }

  @Test
  @DisplayName("GET /api/order should return 200 OK and empty list when there are no orders")
  void shouldReturnEmptyList_WhenNoOrders() throws Exception {
    // Given
    given(orderService.getAllOrders()).willReturn(List.of());

    // When & Then
    mockMvc.perform(get("/api/order"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName("GET /api/order/{id} should return 200 OK and the order when found")
  void shouldReturnOrderById_Return200AndBody() throws Exception {
    // Given
    Orders order = new Orders();
    order.setTotalPrice(99.99);
    order.setStatus(Status.CONFIRMED);
    order.addOrderItem(new OrderItem(5, "gamma-thing", 3, 33.33));

    given(orderService.getOrder(anyInt())).willReturn(order);

    // When & Then
    mockMvc.perform(get("/api/order/{id}", 123))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPrice").value(99.99))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.orderItems[0].itemId").value(5))
        .andExpect(jsonPath("$.orderItems[0].itemName").value("gamma-thing"))
        .andExpect(jsonPath("$.orderItems[0].quantity").value(3))
        .andExpect(jsonPath("$.orderItems[0].price").value(33.33));
  }

  @Test
  @DisplayName("GET /api/order/{id} should return 200 and include multiple items when present")
  void shouldReturnOrderById_WithMultipleItems() throws Exception {
    // Given
    Orders order = new Orders();
    order.setTotalPrice(50.0);
    order.setStatus(Status.SHIPPED);
    order.addOrderItem(new OrderItem(1, "item-1", 1, 10.0));
    order.addOrderItem(new OrderItem(2, "item-2", 2, 20.0));

    given(orderService.getOrder(anyInt())).willReturn(order);

    // When & Then
    mockMvc.perform(get("/api/order/{id}", 7))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.orderItems").isArray())
        .andExpect(jsonPath("$.orderItems.length()")
            .value(2))
        .andExpect(jsonPath("$.orderItems[0].itemId").value(1))
        .andExpect(jsonPath("$.orderItems[1].itemId").value(2));
  }

  @Test
  @DisplayName("GET /api/order/{id} should return 404 Not Found when service returns null")
  void shouldReturn404_WhenOrderNotFound() throws Exception {
    // Given
    given(orderService.getOrder(anyInt())).willReturn(null);

    // When & Then
    mockMvc.perform(get("/api/order/{id}", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/order/{id} with non-integer id should return 400 Bad Request")
  void shouldReturn400_WhenOrderIdIsNotInteger() throws Exception {
    // No service interaction expected; Spring will fail to convert the path variable
    mockMvc.perform(get("/api/order/abc"))
        .andExpect(status().isBadRequest());
  }
}

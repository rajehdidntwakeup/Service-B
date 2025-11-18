package test.serviceb.controller;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.servlet.ServletException;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.service.OrderService;

@WebMvcTest(controllers = OrderController.class)
@AutoConfigureMockMvc(addFilters = false) // disable Spring Security filters for controller slice tests
class OrderControllerUnitTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private OrderService orderService;

  private Orders sampleOrder(double total, Status status) {
    List<OrderItem> items = new ArrayList<>();
    return new Orders(total, status, items);
  }

  @Test
  @DisplayName("POST /api/order - createOrder returns 201 with created order")
  void createOrder_shouldReturn201AndBody() throws Exception {
    // Arrange
    OrderDto dto = new OrderDto(100.0, "CONFIRMED", List.of());
    Orders created = sampleOrder(100.0, Status.CONFIRMED);
    when(orderService.createOrder(any(OrderDto.class))).thenReturn(created);

    // Act & Assert
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isCreated())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPrice").value(100.0))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  @DisplayName("GET /api/order - getAllOrders returns 200 with list")
  void getAllOrders_shouldReturn200AndList() throws Exception {
    // Arrange
    Orders o1 = sampleOrder(50.0, Status.SHIPPED);
    Orders o2 = sampleOrder(75.5, Status.CANCELLED);
    when(orderService.getAllOrders()).thenReturn(List.of(o1, o2));

    // Act & Assert
    mockMvc.perform(get("/api/order"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$[0].totalPrice").value(50.0))
        .andExpect(jsonPath("$[0].status").value("SHIPPED"))
        .andExpect(jsonPath("$[1].totalPrice").value(75.5))
        .andExpect(jsonPath("$[1].status").value("CANCELLED"));
  }

  @Test
  @DisplayName("GET /api/order - getAllOrders returns 200 with empty array when no orders")
  void getAllOrders_shouldReturnEmptyArray() throws Exception {
    when(orderService.getAllOrders()).thenReturn(List.of());

    mockMvc.perform(get("/api/order"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(content().json("[]"));
  }

  @Test
  @DisplayName("GET /api/order/{id} - getOrder returns 200 when found")
  void getOrder_shouldReturn200WhenFound() throws Exception {
    // Arrange
    Orders found = sampleOrder(33.3, Status.CONFIRMED);
    when(orderService.getOrder(1)).thenReturn(found);

    // Act & Assert
    mockMvc.perform(get("/api/order/{id}", 1))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPrice").value(33.3))
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  @DisplayName("GET /api/order/{id} - getOrder returns 404 when not found")
  void getOrder_shouldReturn404WhenNotFound() throws Exception {
    // Arrange
    when(orderService.getOrder(999)).thenReturn(null);

    // Act & Assert
    mockMvc.perform(get("/api/order/{id}", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/order/{id} - non-numeric id yields 400 Bad Request")
  void getOrder_nonNumericId_shouldReturn400() throws Exception {
    mockMvc.perform(get("/api/order/abc"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /api/order/{id} - updateOrder returns 200 when updated")
  void updateOrder_shouldReturn200WhenUpdated() throws Exception {
    // Arrange
    OrderDto update = new OrderDto(120.5, "SHIPPED", List.of());
    Orders updated = sampleOrder(120.5, Status.SHIPPED);
    when(orderService.updateOrder(eq(5), any(OrderDto.class))).thenReturn(updated);

    // Act & Assert
    mockMvc.perform(put("/api/order/{id}", 5)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.totalPrice").value(120.5))
        .andExpect(jsonPath("$.status").value("SHIPPED"));
  }

  @Test
  @DisplayName("PUT /api/order/{id} - updateOrder returns 404 when target not found")
  void updateOrder_shouldReturn404WhenNotFound() throws Exception {
    // Arrange
    OrderDto update = new OrderDto(10.0, "CANCELLED", List.of());
    when(orderService.updateOrder(eq(42), any(OrderDto.class))).thenReturn(null);

    // Act & Assert
    mockMvc.perform(put("/api/order/{id}", 42)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isNotFound());
  }

  // ================= Edge cases: Validation and error handling =================

  @Test
  @DisplayName("POST /api/order - blank status triggers 400 and service is not called")
  void createOrder_blankStatus_shouldReturn400_andNotCallService() throws Exception {
    OrderDto dto = new OrderDto(10.0, "", List.of());

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).createOrder(any(OrderDto.class));
  }

  @Test
  @DisplayName("POST /api/order - negative totalPrice triggers 400 and service is not called")
  void createOrder_negativeTotal_shouldReturn400_andNotCallService() throws Exception {
    OrderDto dto = new OrderDto(-1.0, "CONFIRMED", List.of());

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).createOrder(any(OrderDto.class));
  }

  @Test
  @DisplayName("POST /api/order - invalid JSON returns 400 Bad Request")
  void createOrder_invalidJson_shouldReturn400() throws Exception {
    String invalidJson = "{\"totalPrice\":100.0, \"status\": \"CONFIRMED\", \"items\": [ }"; // malformed

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/order - missing Content-Type yields 415 Unsupported Media Type")
  void createOrder_missingContentType_shouldReturn415() throws Exception {
    OrderDto dto = new OrderDto(10.0, "CONFIRMED", List.of());

    mockMvc.perform(post("/api/order")
            // intentionally no contentType
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("PUT /api/order/{id} - blank status triggers 400 and service is not called")
  void updateOrder_blankStatus_shouldReturn400_andNotCallService() throws Exception {
    OrderDto dto = new OrderDto(5.0, "", List.of());

    mockMvc.perform(put("/api/order/{id}", 7)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).updateOrder(eq(7), any(OrderDto.class));
  }

  @Test
  @DisplayName("PUT /api/order/{id} - negative totalPrice triggers 400 and service is not called")
  void updateOrder_negativeTotal_shouldReturn400_andNotCallService() throws Exception {
    OrderDto dto = new OrderDto(-2.0, "SHIPPED", List.of());

    mockMvc.perform(put("/api/order/{id}", 8)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)))
        .andExpect(status().isBadRequest());

    verify(orderService, never()).updateOrder(eq(8), any(OrderDto.class));
  }

  @Test
  @DisplayName("POST /api/order - service throws runtime exception yields 500")
  void createOrder_serviceThrows_shouldReturn500() {
    OrderDto dto = new OrderDto(10.0, "CONFIRMED", List.of());
    when(orderService.createOrder(any(OrderDto.class))).thenThrow(new RuntimeException("boom"));

    // With no @ControllerAdvice, the exception will propagate and MockMvc will throw ServletException
    assertThrows(ServletException.class, () ->
        mockMvc.perform(post("/api/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
            .andReturn()
    );
  }
}

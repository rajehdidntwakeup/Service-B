package test.serviceb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.service.OrderService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb-controller-update-order-integration-test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UpdateOrderIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private OrderService orderService;

  @Test
  @DisplayName("PUT /api/order/{id} with valid OrderDto should return 200 OK and the updated Order body")
  void shouldUpdateOrder_Return200AndBody() throws Exception {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(77.77);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 7.77, 10)));
    String json = objectMapper.writeValueAsString(dto);

    Orders updated = new Orders();
    updated.setTotalPrice(77.77);
    updated.setStatus(Status.CONFIRMED);
    updated.addOrderItem(new OrderItem(1, "alpha-widget", 10, 7.77));

    given(orderService.updateOrder(anyInt(), any(OrderDto.class))).willReturn(updated);

    // When & Then
    mockMvc.perform(put("/api/order/{id}", 5)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalPrice").value(77.77))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.orderItems[0].itemId").value(1))
        .andExpect(jsonPath("$.orderItems[0].itemName").value("alpha-widget"))
        .andExpect(jsonPath("$.orderItems[0].quantity").value(10))
        .andExpect(jsonPath("$.orderItems[0].price").value(7.77));
  }

  @Test
  @DisplayName("Controller should pass path id and OrderDto to service when updating an order")
  void shouldPassIdAndOrderDtoToService() throws Exception {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(42.0);
    dto.setStatus("SHIPPED");
    dto.setItems(List.of(new OrderItemDto(9, "beta-gadget", 6.0, 7)));
    String json = objectMapper.writeValueAsString(dto);

    Orders updated = new Orders();
    updated.setTotalPrice(42.0);
    updated.setStatus(Status.SHIPPED);
    updated.addOrderItem(new OrderItem(9, "beta-gadget", 7, 6.0));
    given(orderService.updateOrder(anyInt(), any(OrderDto.class))).willReturn(updated);

    // When
    int pathId = 7;
    mockMvc.perform(put("/api/order/{id}", pathId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk());

    // Then
    ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
    verify(orderService).updateOrder(eq(pathId), captor.capture());
    OrderDto captured = captor.getValue();
    assertThat(captured.getTotalPrice()).isEqualTo(42.0);
    assertThat(captured.getStatus()).isEqualTo("SHIPPED");
    assertThat(captured.getItems()).hasSize(1);
    assertThat(captured.getItems().getFirst().getItemId()).isEqualTo(9);
  }

  @Test
  @DisplayName("PUT /api/order/{id} should return 404 Not Found when service returns null")
  void shouldReturn404_WhenServiceReturnsNull() throws Exception {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(10.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 5.0, 1)));
    String json = objectMapper.writeValueAsString(dto);

    given(orderService.updateOrder(anyInt(), any(OrderDto.class))).willReturn(null);

    // When & Then
    mockMvc.perform(put("/api/order/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("PUT /api/order/{id} without Content-Type should return 415 Unsupported Media Type")
  void shouldReturn415_WhenContentTypeMissing() throws Exception {
    String json = "{}";

    mockMvc.perform(put("/api/order/{id}", 1)
            .content(json))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("PUT /api/order/{id} with malformed JSON should return 400 Bad Request")
  void shouldReturn400_WhenMalformedJson() throws Exception {
    String malformed = "{"; // invalid JSON

    mockMvc.perform(put("/api/order/{id}", 3)
            .contentType(MediaType.APPLICATION_JSON)
            .content(malformed))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /api/order/{id} with empty body should return 400 Bad Request")
  void shouldReturn400_WhenBodyEmpty() throws Exception {
    mockMvc.perform(put("/api/order/{id}", 2)
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /api/order/{id} should propagate service ResponseStatusException (e.g., 409)")
  void shouldPropagateResponseStatusExceptionFromService() throws Exception {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(12.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(5, "delta", 4.0, 3)));
    String json = objectMapper.writeValueAsString(dto);

    given(orderService.updateOrder(anyInt(), any(OrderDto.class)))
        .willThrow(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.CONFLICT,
            "Version conflict"));

    // When & Then
    mockMvc.perform(put("/api/order/{id}", 12)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("PUT /api/order/{id} with non-integer id should return 400 Bad Request")
  void shouldReturn400_WhenOrderIdIsNotInteger() throws Exception {
    // No service interaction expected; Spring fails to convert the path variable
    mockMvc.perform(put("/api/order/abc"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT /api/order/{id} with invalid body should NOT call service and return 400")
  void shouldNotCallService_WhenValidationFails() throws Exception {
    // Given an invalid OrderDto payload (fails @Valid): 0 price, blank status, empty items
    String invalidJson = """
        {
          "totalPrice": 0,
          "status": "",
          "items": []
        }
        """;

    // When
    mockMvc.perform(put("/api/order/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest());

    // Then service must not be invoked
    verify(orderService, never()).updateOrder(anyInt(), any(OrderDto.class));
  }

  @Test
  @DisplayName("PUT /api/order/{id} should accept negative id and pass it to the service")
  void shouldPassNegativeIdToService() throws Exception {
    // Given a valid DTO
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(12.34);
    dto.setStatus("NEW");
    dto.setItems(List.of(new OrderItemDto(2, "neg-item", 1.23, 4)));
    String json = objectMapper.writeValueAsString(dto);

    Orders updated = new Orders();
    updated.setTotalPrice(12.34);
    updated.setStatus(Status.CONFIRMED);
    updated.addOrderItem(new OrderItem(2, "neg-item", 4, 1.23));
    given(orderService.updateOrder(anyInt(), any(OrderDto.class))).willReturn(updated);

    int negativeId = -5;
    mockMvc.perform(put("/api/order/{id}", negativeId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    verify(orderService).updateOrder(eq(negativeId), any(OrderDto.class));
  }
}

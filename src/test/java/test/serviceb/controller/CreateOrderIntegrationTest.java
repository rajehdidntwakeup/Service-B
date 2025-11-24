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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:testdb-controller-create-order-integration-test")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreateOrderIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private OrderService orderService;

  @Test
  @DisplayName("POST /api/order with valid OrderDto should return 201 Created and the created Order body")
  void shouldCreateOrder_Return201AndBody() throws Exception {
    // Given
    String payload = """
        {
          "totalPrice": 99.99,
          "status": "CONFIRMED",
          "items": [
            {"itemId": 1, "itemName": "alpha-widget", "price": 9.99, "quantity": 3}
          ]
        }
        """;

    Orders saved = new Orders();
    saved.setTotalPrice(99.99);
    saved.setStatus(Status.CONFIRMED);
    // simulate persistence id and item
    OrderItem oi = new OrderItem(1, "alpha-widget", 3, 9.99);
    saved.addOrderItem(oi);
    // mock service
    given(orderService.createOrder(any(OrderDto.class))).willReturn(saved);

    // When & Then
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.totalPrice").value(99.99))
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.orderItems[0].itemId").value(1))
        .andExpect(jsonPath("$.orderItems[0].itemName").value("alpha-widget"))
        .andExpect(jsonPath("$.orderItems[0].quantity").value(3))
        .andExpect(jsonPath("$.orderItems[0].price").value(9.99));
  }

  @Test
  @DisplayName("Controller should pass OrderDto to service when creating an order")
  void shouldPassOrderDtoToService() throws Exception {
    // Given a valid DTO
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(42.5);
    dto.setStatus("SHIPPED");
    dto.setItems(List.of(new OrderItemDto(7, "beta-gadget", 4.25, 10)));
    String json = objectMapper.writeValueAsString(dto);

    // and a mocked service response
    Orders saved = new Orders();
    saved.setTotalPrice(42.5);
    saved.setStatus(Status.SHIPPED);
    saved.addOrderItem(new OrderItem(7, "beta-gadget", 10, 4.25));
    given(orderService.createOrder(any(OrderDto.class))).willReturn(saved);

    // When
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated());

    // Then capture and assert the DTO passed into the service
    ArgumentCaptor<OrderDto> captor = ArgumentCaptor.forClass(OrderDto.class);
    verify(orderService).createOrder(captor.capture());
    OrderDto captured = captor.getValue();
    assertThat(captured.getTotalPrice()).isEqualTo(42.5);
    assertThat(captured.getStatus()).isEqualTo("SHIPPED");
    assertThat(captured.getItems()).hasSize(1);
    assertThat(captured.getItems().getFirst().getItemId()).isEqualTo(7);
    assertThat(captured.getItems().getFirst().getItemName()).isEqualTo("beta-gadget");
    assertThat(captured.getItems().getFirst().getPrice()).isEqualTo(4.25);
    assertThat(captured.getItems().getFirst().getQuantity()).isEqualTo(10);
  }

  @Test
  @DisplayName("POST /api/order without Content-Type should return 415 Unsupported Media Type")
  void shouldReturn415_WhenContentTypeMissing() throws Exception {
    String json = "{}";

    mockMvc.perform(post("/api/order")
            .content(json))
        .andExpect(status().isUnsupportedMediaType());
  }

  @Test
  @DisplayName("POST /api/order with malformed JSON should return 400 Bad Request")
  void shouldReturn400_WhenMalformedJson() throws Exception {
    String malformed = "{"; // invalid JSON

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(malformed))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/order with empty body should return 400 Bad Request")
  void shouldReturn400_WhenBodyEmpty() throws Exception {
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /api/order should propagate service ResponseStatusException (e.g., 404)")
  void shouldPropagateResponseStatusExceptionFromService() throws Exception {
    // Given a valid DTO
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(10.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 5.0, 1)));
    String json = objectMapper.writeValueAsString(dto);

    // Service throws a ResponseStatusException
    given(orderService.createOrder(any(OrderDto.class)))
        .willThrow(new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND,
            "Item with ID 1 not found"));

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("POST /api/order accepts zero-priced item (price=0.0) and returns it in response")
  void shouldAcceptZeroPriceItem() throws Exception {
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(15.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(3, "gamma-widget", 0.0, 3)));
    String json = objectMapper.writeValueAsString(dto);

    Orders saved = new Orders();
    saved.setTotalPrice(15.0);
    saved.setStatus(Status.CONFIRMED);
    saved.addOrderItem(new OrderItem(3, "gamma-widget", 3, 0.0));
    given(orderService.createOrder(any(OrderDto.class))).willReturn(saved);

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderItems[0].price").value(0.0));
  }

  @Test
  @DisplayName("POST /api/order with multiple items should return all items in response")
  void shouldReturnMultipleItems() throws Exception {
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(30.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(
        new OrderItemDto(11, "alpha-widget", 5.0, 2),
        new OrderItemDto(22, "beta-gadget", 10.0, 1)
    ));
    String json = objectMapper.writeValueAsString(dto);

    Orders saved = new Orders();
    saved.setTotalPrice(30.0);
    saved.setStatus(Status.CONFIRMED);
    saved.addOrderItem(new OrderItem(11, "alpha-widget", 2, 5.0));
    saved.addOrderItem(new OrderItem(22, "beta-gadget", 1, 10.0));
    given(orderService.createOrder(any(OrderDto.class))).willReturn(saved);

    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.orderItems.length()").value(2))
        .andExpect(jsonPath("$.orderItems[0].itemId").value(11))
        .andExpect(jsonPath("$.orderItems[1].itemId").value(22));
  }
}

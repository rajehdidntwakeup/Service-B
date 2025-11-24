package test.serviceb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    // Provide two external inventory mappings used by OrderServiceImpl
    "external.inventory.externalInventory.alpha=alpha,http://alpha.example",
    "external.inventory.externalInventory.beta=beta,http://beta.example",
    // Isolate datasource per test class
    "spring.datasource.url=jdbc:h2:mem:testdb-service-create-order-integration"
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Ensure hibernate creates schema in tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CreateOrderIntegrationTest {

  @Autowired
  private OrderService orderService;

  @Autowired
  private OrdersRepository ordersRepository;

  @Autowired
  private ObjectMapper objectMapper;

  @BeforeEach
  void cleanDb() {
    ordersRepository.deleteAll();
  }

  @Test
  @DisplayName("createOrder should default unknown status to CONFIRMED")
  void createOrder_unknownStatus_defaultsToConfirmed() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(12.34);
    dto.setStatus("weird-status"); // not in enum, should map to CONFIRMED by default
    dto.setItems(List.of(new OrderItemDto(5, "alpha-thing", 1.23, 1)));

    // When
    Orders saved = orderService.createOrder(dto);

    // Then
    assertThat(saved.getStatus()).isEqualTo(Status.CONFIRMED);
  }

  @Test
  @DisplayName("createOrder should throw when status is null")
  void createOrder_nullStatus_throws() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(10.0);
    dto.setStatus(null); // will cause NPE in getStatusFromOrderDto
    dto.setItems(List.of(new OrderItemDto(6, "beta-thing", 2.0, 1)));

    // When / Then
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(NullPointerException.class);

    // Ensure nothing persisted
    assertThat(ordersRepository.count()).isZero();
  }

  @Test
  @DisplayName("createOrder should persist with zero items when items list is empty")
  void createOrder_emptyItems_persistsWithZeroItems() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(0.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of());

    // When
    Orders saved = orderService.createOrder(dto);

    // Then
    assertThat(saved.getOrderItems()).isEmpty();
    assertThat(ordersRepository.findAll()).hasSize(1);
  }

  @Test
  @DisplayName("createOrder should fail when requested quantity exceeds available stock")
  void createOrder_quantityExceedsStock_throws() {
    // Given: stub returns stock=100, request 101
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(1.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(7, "alpha-widget", 0.5, 101)));

    // When / Then
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to fetch item with ID: 7");

    // Ensure nothing persisted
    assertThat(ordersRepository.count()).isZero();
  }

  @Test
  @DisplayName("createOrder should fail when item name is null (cannot resolve external client)")
  void createOrder_nullItemName_throws() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(3.0);
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(8, null, 1.0, 1)));

    // When / Then
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to fetch item with ID: 8");

    // Ensure nothing persisted
    assertThat(ordersRepository.count()).isZero();
  }

  @Test
  @DisplayName("createOrder should allow negative item price (documents current behavior)")
  void createOrder_negativePrice_persistsAsIs() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(-5.0); // service does not validate; this documents current behavior
    dto.setStatus("CONFIRMED");
    dto.setItems(List.of(new OrderItemDto(9, "beta-gadget", -2.5, 1)));

    // When
    Orders saved = orderService.createOrder(dto);

    // Then
    assertThat(saved.getOrderItems()).hasSize(1);
    OrderItem oi = saved.getOrderItems().getFirst();
    assertThat(oi.getPrice()).isEqualTo(-2.5);
    assertThat(saved.getTotalPrice()).isEqualTo(-5.0);
  }

  @Test
  @DisplayName("createOrder should persist and return order with mapped status and created items (happy path)")
  @Transactional
  void createOrder_happyPath_persistsAndReturns() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(99.99);
    dto.setStatus("shipped"); // lower-case to test case-insensitive mapping
    dto.setItems(List.of(
        new OrderItemDto(1, "alpha-widget", 9.99, 3)
    ));

    // When
    Orders saved = orderService.createOrder(dto);

    // Then
    assertThat(saved.getId()).isPositive();
    assertThat(saved.getTotalPrice()).isEqualTo(99.99);
    assertThat(saved.getStatus()).isEqualTo(Status.SHIPPED);
    assertThat(saved.getOrderItems()).hasSize(1);
    OrderItem oi = saved.getOrderItems().getFirst();
    assertThat(oi.getItemId()).isEqualTo(1);
    assertThat(oi.getItemName()).isEqualTo("alpha-widget");
    assertThat(oi.getQuantity()).isEqualTo(3);
    assertThat(oi.getPrice()).isEqualTo(9.99);

    // And persisted in repository
    List<Orders> all = ordersRepository.findAll();
    assertThat(all).hasSize(1);
    assertThat(all.getFirst().getOrderItems()).hasSize(1);
  }

  @Test
  @DisplayName("createOrder should ignore items with zero or negative quantity")
  void createOrder_ignoresNonPositiveQuantities() {
    // Given
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(10.0);
    dto.setStatus("confirmed");
    dto.setItems(List.of(
        new OrderItemDto(2, "beta-gadget", 5.0, 0),
        new OrderItemDto(3, "alpha-widget", 5.0, -1),
        new OrderItemDto(4, "beta-gadget", 2.5, 2) // only this should be created
    ));

    // When
    Orders saved = orderService.createOrder(dto);

    // Then
    assertThat(saved.getOrderItems()).hasSize(1);
    OrderItem oi = saved.getOrderItems().getFirst();
    assertThat(oi.getItemId()).isEqualTo(4);
    assertThat(oi.getItemName()).isEqualTo("beta-gadget");
    assertThat(oi.getQuantity()).isEqualTo(2);
  }

  @Test
  @DisplayName("createOrder should throw when no matching external inventory client is found for item name")
  void createOrder_throwsWhenNoMatchingClient() {
    // The given item name doesn't contain any configured keys (alpha/beta)
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(1.0);
    dto.setStatus("confirmed");
    dto.setItems(List.of(new OrderItemDto(10, "unknown-widget", 1.0, 1)));

    // When/Then
    assertThatThrownBy(() -> orderService.createOrder(dto))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to fetch item with ID: 10");

    // Ensure nothing persisted
    assertThat(ordersRepository.count()).isZero();
  }

  /**
   * Test configuration that provides a WebClient.Builder bean with a stubbed ExchangeFunction.
   * This avoids real HTTP calls while exercising the WebClient path used by OrderServiceImpl.
   */
  @org.springframework.boot.test.context.TestConfiguration
  static class WebClientStubConfig {

    @Bean
    WebClient.Builder webClientBuilder(ObjectMapper mapper) {
      ExchangeFunction exchange = request -> Mono.just(buildResponse(mapper, request));
      // Return standard builder configured with our stubbed exchange function.
      return WebClient.builder().exchangeFunction(exchange);
    }

    private static ClientResponse buildResponse(ObjectMapper mapper, ClientRequest request) {
      URI uri = request.url();
      String path = uri.getPath();
      HttpStatus status = HttpStatus.OK;
      Object body;

      // Simple routing based on method and path
      if (request.method().matches("GET")) {
        // Expecting pattern: /{id}/itemname/{name}
        String[] parts = path.split("/");
        int id = Integer.parseInt(parts[1]);
        String name = parts[3];
        body = new test.serviceb.domain.Item(id, name, 100, 9.99, "stub-item");
      } else if (request.method().matches("PUT")) {
        // Respond with an InventoryItemDto-like body
        body = new test.serviceb.domain.dto.InventoryItemDto();
        // We don't need to fill fields for the service flow in createOrderItem
      } else {
        status = HttpStatus.METHOD_NOT_ALLOWED;
        body = new ErrorMessage("Method not allowed");
      }

      byte[] json;
      try {
        json = mapper.writeValueAsBytes(body);
      } catch (Exception e) {
        json = ("{}\n").getBytes(StandardCharsets.UTF_8);
      }

      DefaultDataBufferFactory factory = new DefaultDataBufferFactory();
      DataBuffer buffer = factory.wrap(json);
      return ClientResponse.create(status)
          .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
          .body(Flux.just(buffer))
          .build();
    }

    record ErrorMessage(String message) {
    }
  }
}

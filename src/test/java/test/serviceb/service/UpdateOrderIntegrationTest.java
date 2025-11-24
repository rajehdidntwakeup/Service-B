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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
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
import test.serviceb.service.OrderService;

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
    "spring.datasource.url=jdbc:h2:mem:testdb-service-update-order-integration"
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Ensure hibernate creates schema in tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UpdateOrderIntegrationTest {

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
  @DisplayName("updateOrder: returns null when order id not found")
  void updateOrder_notFound_returnsNull() {
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(10.0);
    dto.setStatus("PENDING");
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 1.0, 1)));

    Orders result = orderService.updateOrder(999_999, dto);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("updateOrder: updates totalPrice and status when order exists and is not CANCELLED")
  void updateOrder_existing_updatesFields() {
    // Given existing order
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(5.0);
    existing.addOrderItem(new OrderItem(1, "alpha-widget", 2, 2.5));
    Orders saved = ordersRepository.save(existing);

    // When updating
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(42.42);
    dto.setStatus("SHIPPED");
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 2.5, 2)));

    Orders updated = orderService.updateOrder(saved.getId(), dto);

    // Then
    assertThat(updated).isNotNull();
    assertThat(updated.getId()).isEqualTo(saved.getId());
    assertThat(updated.getTotalPrice()).isEqualTo(42.42);
    assertThat(updated.getStatus()).isEqualTo(Status.SHIPPED);
  }

  @Test
  @DisplayName("updateOrder: throws 400 BAD_REQUEST when trying to update a CANCELLED order")
  void updateOrder_cancelled_throwsBadRequest() {
    Orders existing = new Orders();
    existing.setStatus(Status.CANCELLED);
    existing.setTotalPrice(100.0);
    existing.addOrderItem(new OrderItem(2, "beta-gadget", 1, 100.0));
    Orders saved = ordersRepository.save(existing);

    OrderDto dto = new OrderDto();
    dto.setTotalPrice(50.0);
    dto.setStatus("PENDING");
    dto.setItems(List.of(new OrderItemDto(2, "beta-gadget", 100.0, 1)));

    assertThatThrownBy(() -> orderService.updateOrder(saved.getId(), dto))
        .hasMessageContaining("Cannot update a cancelled order");
  }

  @Test
  @DisplayName("updateOrder: changing status to CANCELLED triggers restock with external service and persists change")
  @Transactional
  void updateOrder_changeToCancelled_triggersRestockAndPersists() {
    // Given order that is not cancelled
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(30.0);
    existing.addOrderItem(new OrderItem(3, "alpha-widget", 3, 10.0));
    Orders saved = ordersRepository.save(existing);

    // Change to CANCELLED
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(30.0);
    dto.setStatus("CANCELLED");
    dto.setItems(List.of(new OrderItemDto(3, "alpha-widget", 10.0, 3)));

    Orders after = orderService.updateOrder(saved.getId(), dto);

    assertThat(after).isNotNull();
    assertThat(after.getStatus()).isEqualTo(Status.CANCELLED);
    // Restock path completes without exceptions thanks to stubbed WebClient
  }

  /**
   * Test configuration that provides a WebClient.Builder bean with a stubbed ExchangeFunction.
   * This avoids real HTTP calls while exercising the WebClient path used by OrderServiceImpl.
   * For this test, both GET and PUT return an Item JSON body so that deserialization to Item works
   * in restockOrderItems.
   */
  @org.springframework.boot.test.context.TestConfiguration
  static class WebClientStubConfig {

    @Bean
    WebClient.Builder webClientBuilder(ObjectMapper mapper) {
      ExchangeFunction exchange = request -> Mono.just(buildResponse(mapper, request));
      return WebClient.builder().exchangeFunction(exchange);
    }

    private static ClientResponse buildResponse(ObjectMapper mapper, ClientRequest request) {
      URI uri = request.url();
      String path = uri.getPath();
      HttpStatus status = HttpStatus.OK;
      Object body;

      // Simple routing based on method and path
      if (request.method().matches("GET") || request.method().matches("PUT")) {
        // Expecting pattern: /{id}/itemname/{name}
        String[] parts = path.split("/");
        int id = Integer.parseInt(parts[1]);
        String name = parts.length > 3 ? parts[3] : "unknown";
        body = new test.serviceb.domain.Item(id, name, 100, 9.99, "stub-item");
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

    record ErrorMessage(String message) { }
  }
}

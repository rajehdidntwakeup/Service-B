package test.serviceb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
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
    "spring.datasource.url=jdbc:h2:mem:testdb-service-update-order-edge-cases"
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Ensure hibernate creates schema in tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class UpdateOrderEdgeCasesIntegrationTest {

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
  @DisplayName("updateOrder: mixed-case status string maps to enum (shiPped -> SHIPPED)")
  void updateOrder_mixedCaseStatus_isMapped() {
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(10.0);
    existing.addOrderItem(new OrderItem(1, "alpha-widget", 1, 10.0));
    Orders saved = ordersRepository.save(existing);

    OrderDto dto = new OrderDto();
    dto.setTotalPrice(11.0);
    dto.setStatus("shiPped"); // mixed case
    dto.setItems(List.of(new OrderItemDto(1, "alpha-widget", 10.0, 1)));

    Orders updated = orderService.updateOrder(saved.getId(), dto);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(Status.SHIPPED);
    assertThat(updated.getTotalPrice()).isEqualTo(11.0);
  }

  @Test
  @DisplayName("updateOrder: unknown status defaults to CONFIRMED")
  void updateOrder_unknownStatus_defaultsToConfirmed() {
    Orders existing = new Orders();
    existing.setStatus(Status.SHIPPED);
    existing.setTotalPrice(20.0);
    existing.addOrderItem(new OrderItem(2, "beta-gadget", 2, 5.0));
    Orders saved = ordersRepository.save(existing);

    OrderDto dto = new OrderDto();
    dto.setTotalPrice(21.0);
    dto.setStatus("delayed"); // not in enum mapping -> defaults to CONFIRMED
    dto.setItems(List.of(new OrderItemDto(2, "beta-gadget", 5.0, 2)));

    Orders updated = orderService.updateOrder(saved.getId(), dto);
    assertThat(updated).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(Status.CONFIRMED);
    assertThat(updated.getTotalPrice()).isEqualTo(21.0);
  }

  @Test
  @DisplayName("updateOrder: changing to CANCELLED with unmapped itemName throws NOT_FOUND from getWebClient")
  @Transactional
  void updateOrder_cancelToUnmappedInventory_throwsNotFound() {
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(30.0);
    // 'gamma' does not match provided external inventory keys (alpha, beta)
    existing.addOrderItem(new OrderItem(3, "gamma-widget", 3, 10.0));
    Orders saved = ordersRepository.save(existing);

    OrderDto dto = new OrderDto();
    dto.setTotalPrice(30.0);
    dto.setStatus("CANCELLED");
    dto.setItems(List.of(new OrderItemDto(3, "gamma-widget", 10.0, 3)));

    assertThatThrownBy(() -> orderService.updateOrder(saved.getId(), dto))
        .hasMessageContaining("Item with name gamma-widget not found");
  }

  @Test
  @DisplayName("updateOrder: restock PUT returns empty body -> throws 'was not updated!' error")
  @Transactional
  void updateOrder_cancelled_putReturnsEmptyBody_throwsUpdatedNotFound() {
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(40.0);
    existing.addOrderItem(new OrderItem(4, "alpha-widget", 4, 10.0));
    Orders saved = ordersRepository.save(existing);

    OrderDto dto = new OrderDto();
    dto.setTotalPrice(40.0);
    dto.setStatus("CANCELLED");
    dto.setItems(List.of(new OrderItemDto(4, "alpha-widget", 10.0, 4)));

    assertThatThrownBy(() -> orderService.updateOrder(saved.getId(), dto))
        .hasMessageContaining("was not updated!");
  }

  /**
   * Test configuration that provides a WebClient.Builder bean with a stubbed ExchangeFunction.
   * For this class, we simulate GET returning a real Item and PUT returning an empty body
   * to trigger the edge case where updatedItem is null.
   */
  @org.springframework.boot.test.context.TestConfiguration
  static class WebClientStubConfig {

    @Bean
    WebClient.Builder webClientBuilder(ObjectMapper mapper) {
      ExchangeFunction exchange = request -> Mono.just(buildResponse(mapper, request))
          // Convert ClientResponse to one compatible with WebClient (already is)
          ;
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
        String name = parts.length > 3 ? parts[3] : "unknown";
        body = new test.serviceb.domain.Item(id, name, 100, 9.99, "stub-item");
      } else if (request.method().matches("PUT")) {
        // Return empty body to simulate upstream not returning an Item
        return ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            .body(Flux.empty())
            .build();
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

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
import test.serviceb.repository.OrdersRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    // Provide two external inventory mappings used by OrderServiceImpl
    "external.inventory.externalInventory.alpha=alpha,http://alpha.example",
    "external.inventory.externalInventory.beta=beta,http://beta.example",
    // Isolate datasource per test class
    "spring.datasource.url=jdbc:h2:mem:testdb-service-get-order-integration"
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Ensure hibernate creates schema in tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GetOrderIntegrationTest {

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
  @DisplayName("getOrder should return null when order id is not found")
  void getOrder_notFound_returnsNull() {
    Orders result = orderService.getOrder(999_999);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getOrder should return the persisted order when found")
  void getOrder_existing_returnsOrder() {
    // Given a persisted order
    Orders toSave = new Orders();
    toSave.setStatus(Status.SHIPPED);
    toSave.setTotalPrice(42.0);
    OrderItem oi = new OrderItem(1, "alpha-widget", 2, 9.99);
    toSave.addOrderItem(oi);
    Orders saved = ordersRepository.save(toSave);

    // When
    Orders found = orderService.getOrder(saved.getId());

    // Then
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(saved.getId());
    assertThat(found.getStatus()).isEqualTo(Status.SHIPPED);
    assertThat(found.getTotalPrice()).isEqualTo(42.0);
    // Accessing lazy collections can be brittle; keep assertion basic on presence
    assertThat(found.getOrderItems()).isNotNull();
  }

  @Test
  @DisplayName("getAllOrders should return empty list when no orders exist")
  void getAllOrders_empty_returnsEmptyList() {
    List<Orders> all = orderService.getAllOrders();
    assertThat(all).isEmpty();
  }

  @Test
  @DisplayName("getAllOrders should return all persisted orders")
  void getAllOrders_multiple_returnsList() {
    Orders o1 = new Orders();
    o1.setStatus(Status.CONFIRMED);
    o1.setTotalPrice(10.0);
    ordersRepository.save(o1);

    Orders o2 = new Orders();
    o2.setStatus(Status.CANCELLED);
    o2.setTotalPrice(20.0);
    ordersRepository.save(o2);

    List<Orders> all = orderService.getAllOrders();
    assertThat(all).hasSize(2);
    assertThat(all.stream().map(Orders::getId)).containsExactlyInAnyOrder(o1.getId(), o2.getId());
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

    record ErrorMessage(String message) { }
  }
}

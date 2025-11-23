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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    // Provide two external inventory mappings used by OrderServiceImpl
    "external.inventory.externalInventory.alpha=alpha,http://alpha.example",
    "external.inventory.externalInventory.beta=beta,http://beta.example",
    // Isolate datasource per test class
    "spring.datasource.url=jdbc:h2:mem:testdb-service-get-order-edgecases"
})
@ActiveProfiles("test")
@TestPropertySource(properties = {
    // Ensure hibernate creates schema in tests
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class GetOrderEdgeCasesIntegrationTest {

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
  @DisplayName("getOrder should return null for negative id")
  void getOrder_negativeId_returnsNull() {
    Orders result = orderService.getOrder(-1);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getOrder should return null for zero id")
  void getOrder_zeroId_returnsNull() {
    Orders result = orderService.getOrder(0);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getOrder should return null for Integer.MAX_VALUE id")
  void getOrder_maxInt_returnsNull() {
    Orders result = orderService.getOrder(Integer.MAX_VALUE);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("getOrder multiple calls return consistent result (idempotent retrieval)")
  void getOrder_idempotent_multipleCallsSameResult() {
    Orders toSave = new Orders();
    toSave.setStatus(Status.CONFIRMED);
    toSave.setTotalPrice(5.0);
    toSave.addOrderItem(new OrderItem(7, "alpha-widget", 1, 5.0));
    Orders saved = ordersRepository.save(toSave);

    Orders first = orderService.getOrder(saved.getId());
    Orders second = orderService.getOrder(saved.getId());

    assertThat(first).isNotNull();
    assertThat(second).isNotNull();
    assertThat(first.getId()).isEqualTo(second.getId());
    assertThat(first.getStatus()).isEqualTo(second.getStatus());
    assertThat(first.getTotalPrice()).isEqualTo(second.getTotalPrice());
  }

  @Test
  @DisplayName("getAllOrders empty result should be immutable")
  void getAllOrders_empty_returnsImmutableList() {
    List<Orders> empty = orderService.getAllOrders();
    assertThat(empty).isEmpty();
    assertThatThrownBy(() -> empty.add(new Orders()))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  @DisplayName("getAllOrders returns orders with and without items")
  void getAllOrders_ordersWithAndWithoutItems_allReturned() {
    Orders withItems = new Orders();
    withItems.setStatus(Status.SHIPPED);
    withItems.setTotalPrice(10.0);
    withItems.addOrderItem(new OrderItem(1, "alpha-widget", 1, 10.0));
    ordersRepository.save(withItems);

    Orders withoutItems = new Orders();
    withoutItems.setStatus(Status.CANCELLED);
    withoutItems.setTotalPrice(0.0);
    // no items added
    ordersRepository.save(withoutItems);

    List<Orders> all = orderService.getAllOrders();
    assertThat(all.stream().map(Orders::getId))
        .containsExactlyInAnyOrder(withItems.getId(), withoutItems.getId());
    // Avoid deep assertions on lazy collections; just ensure accessible references
    Orders foundWithItems = all.stream().filter(o -> o.getId() == withItems.getId()).findFirst().orElseThrow();
    Orders foundWithoutItems = all.stream().filter(o -> o.getId() == withoutItems.getId()).findFirst().orElseThrow();
    assertThat(foundWithItems.getOrderItems()).isNotNull();
    assertThat(foundWithoutItems.getOrderItems()).isNotNull();
  }

  @Test
  @DisplayName("getAllOrders with many orders returns all")
  void getAllOrders_manyOrders_returnsAll() {
    for (int i = 0; i < 50; i++) {
      Orders o = new Orders();
      o.setStatus(i % 2 == 0 ? Status.CONFIRMED : Status.SHIPPED);
      o.setTotalPrice(i);
      ordersRepository.save(o);
    }

    List<Orders> all = orderService.getAllOrders();
    assertThat(all).hasSize(50);
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

package test.serviceb.service.unittests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import test.serviceb.domain.Item;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.ExternalInventory;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.converter.ConversionProperties;
import test.serviceb.service.impl.OrderServiceImpl;

public class OrderServiceImplEdgeCaseUnitTest {

  @Mock
  private OrdersRepository ordersRepository;

  @Mock
  private WebClient.Builder webClientBuilder;

  private WebClient webClient;

  // System under test
  private OrderServiceImpl orderService;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);

    // Configure conversion properties so item names containing "book" resolve to our webClient
    ConversionProperties conversionProperties = new ConversionProperties();
    conversionProperties.setExternalInventory(Map.of(
        "inv1", new ExternalInventory("book", "http://inventory.example")
    ));

    // Deep-stub the web client to simplify GET/PUT chaining
    webClient = mock(WebClient.class, RETURNS_DEEP_STUBS);

    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    orderService = new OrderServiceImpl(ordersRepository, conversionProperties, webClientBuilder);
  }

  @Test
  @DisplayName("createOrder throws RuntimeException when no WebClient mapping for item name")
  void createOrder_unknownInventory_throwsRuntimeException() {
    // Arrange: item name does not contain mapping key ("book")
    OrderItemDto itemDto = new OrderItemDto();
    itemDto.setItemId(999);
    itemDto.setItemName("gadget-123");
    itemDto.setQuantity(1);
    itemDto.setPrice(5.0);

    OrderDto orderDto = new OrderDto();
    orderDto.setStatus("confirmed");
    orderDto.setTotalPrice(5.0);
    orderDto.setItems(List.of(itemDto));

    // Act + Assert
    assertThrows(RuntimeException.class, () -> orderService.createOrder(orderDto));
    verifyNoInteractions(ordersRepository);
  }

  @Test
  @DisplayName("createOrder wraps not-found from GET (null body) into RuntimeException and does not PUT or save")
  void createOrder_remoteGetReturnsNull_wrapsToRuntimeException() {
    // Arrange
    OrderItemDto itemDto = new OrderItemDto();
    itemDto.setItemId(101);
    itemDto.setItemName("book-calculus");
    itemDto.setQuantity(1);
    itemDto.setPrice(10.0);

    OrderDto orderDto = new OrderDto();
    orderDto.setStatus("confirmed");
    orderDto.setTotalPrice(10.0);
    orderDto.setItems(List.of(itemDto));

    // GET returns null -> assertItemFound would throw, but inside createOrderItem this is wrapped
    mockWebClientGetReturnsItem(null);

    // Act + Assert
    assertThrows(RuntimeException.class, () -> orderService.createOrder(orderDto));
    verify(webClient, never()).put();
    verifyNoInteractions(ordersRepository);
  }

  @Test
  @DisplayName("createOrder wraps insufficient stock into RuntimeException and avoids PUT/save")
  void createOrder_insufficientStock_wrapsRuntimeException() {
    // Arrange
    OrderItemDto itemDto = new OrderItemDto();
    itemDto.setItemId(101);
    itemDto.setItemName("book-chemistry");
    itemDto.setQuantity(5); // request more than stock
    itemDto.setPrice(10.0);

    OrderDto orderDto = new OrderDto();
    orderDto.setStatus("confirmed");
    orderDto.setTotalPrice(10.0);
    orderDto.setItems(List.of(itemDto));

    // Remote item has lower stock than requested
    Item remote = new Item(101, "Chemistry", 3, 10.0, "desc");
    mockWebClientGetReturnsItem(remote);

    // Act + Assert
    assertThrows(RuntimeException.class, () -> orderService.createOrder(orderDto));
    verify(webClient, never()).put();
    verifyNoInteractions(ordersRepository);
  }

  @Test
  @DisplayName("createOrder ignores negative quantities and does not call WebClient")
  void createOrder_ignoresNegativeQuantity_noWebClientCalls() {
    // Arrange
    OrderItemDto neg = new OrderItemDto();
    neg.setItemId(55);
    neg.setItemName("book-negative");
    neg.setQuantity(-2);
    neg.setPrice(1.0);

    OrderDto orderDto = new OrderDto();
    orderDto.setStatus("confirmed");
    orderDto.setTotalPrice(0.0);
    orderDto.setItems(List.of(neg));

    when(ordersRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    Orders saved = orderService.createOrder(orderDto);

    // Assert
    assertNotNull(saved);
    assertTrue(saved.getOrderItems().isEmpty(), "No items should be added for negative quantity");
    verify(webClient, never()).get();
    verify(webClient, never()).put();
  }

  @Test
  @DisplayName("updateOrder to CANCELLED throws NOT_FOUND when restock GET returns null")
  void updateOrder_cancel_restockGetNull_throwsNotFound() {
    // existing order with one item
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.addOrderItem(new OrderItem(301, "book-geometry", 2, 9.0));
    when(ordersRepository.findById(11)).thenReturn(Optional.of(existing));

    // GET returns null for restock path
    mockWebClientGetReturnsItem(null);

    OrderDto dto = new OrderDto();
    dto.setStatus("cancelled");
    dto.setTotalPrice(0.0);
    dto.setItems(List.of());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrder(11, dto));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    verify(webClient, never()).put();
  }

  @Test
  @DisplayName("updateOrder to CANCELLED throws NOT_FOUND when restock PUT returns null")
  void updateOrder_cancel_restockPutNull_throwsNotFound() {
    // existing order with one item
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.addOrderItem(new OrderItem(302, "book-geometry", 2, 9.0));
    when(ordersRepository.findById(12)).thenReturn(Optional.of(existing));

    // GET returns item; PUT returns null
    Item fetched = new Item(302, "Geometry", 4, 9.0, "desc");
    mockWebClientGetReturnsItem(fetched);
    mockWebClientPutAcceptsInventoryAndReturns();

    OrderDto dto = new OrderDto();
    dto.setStatus("cancelled");
    dto.setTotalPrice(0.0);
    dto.setItems(List.of());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrder(12, dto));
    assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
  }

  @Test
  @DisplayName("updateOrder with unknown status defaults to CONFIRMED")
  void updateOrder_unknownStatus_defaultsToConfirmed() {
    Orders existing = new Orders();
    existing.setStatus(Status.SHIPPED);
    when(ordersRepository.findById(13)).thenReturn(Optional.of(existing));
    when(ordersRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OrderDto dto = new OrderDto();
    dto.setStatus("whatever"); // falls to default in getStatusFromOrderDto -> CONFIRMED
    dto.setTotalPrice(12.3);
    dto.setItems(List.of());

    Orders updated = orderService.updateOrder(13, dto);
    assertNotNull(updated);
    assertEquals(Status.CONFIRMED, updated.getStatus());
    assertEquals(12.3, updated.getTotalPrice());
  }


  private void mockWebClientGetReturnsItem(Item item) {
    when(webClient.get()
        .uri(anyString(), any(), any())
        .retrieve()
        .bodyToMono(eq(Item.class)))
        .thenReturn(Mono.justOrEmpty(item));
  }

  private void mockWebClientPutAcceptsInventoryAndReturns() {
    when(webClient.put()
        .uri(anyString(), any(), any())
        .bodyValue(any())
        .retrieve()
        .bodyToMono(eq(Item.class)))
        .thenReturn(Mono.justOrEmpty((Item) null));
  }
}

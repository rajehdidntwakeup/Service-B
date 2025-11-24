package test.serviceb.service.unittests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import test.serviceb.domain.dto.InventoryItemDto;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.impl.OrderServiceImpl;
import test.serviceb.service.converter.ConversionProperties;

/**
 * Unit tests for {@link OrderServiceImpl}.
 */
public class OrderServiceImplTest {

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

  // Helper to prepare WebClient GET -> Item and PUT -> variant response
  private void mockWebClientGetReturnsItem(Item item) {
    when(webClient.get()
        .uri(anyString(), any(), any())
        .retrieve()
        .bodyToMono(eq(Item.class)))
        .thenReturn(Mono.just(item));
  }

  private void mockWebClientPutAcceptsInventoryAndReturns(Class<?> returnType, Object body) {
    if (returnType == InventoryItemDto.class) {
      when(webClient.put()
          .uri(anyString(), any(), any())
          .bodyValue(any())
          .retrieve()
          .bodyToMono(eq(InventoryItemDto.class)))
          .thenReturn(Mono.just((InventoryItemDto) body));
    } else if (returnType == Item.class) {
      when(webClient.put()
          .uri(anyString(), any(), any())
          .bodyValue(any())
          .retrieve()
          .bodyToMono(eq(Item.class)))
          .thenReturn(Mono.just((Item) body));
    }
  }

  @Test
  @DisplayName("createOrder should save order with items having positive quantity")
  void createOrder_happyPath() {
    // Arrange
    OrderItemDto itemDto = new OrderItemDto();
    itemDto.setItemId(101);
    itemDto.setItemName("book-physics"); // contains key "book"
    itemDto.setQuantity(2);
    itemDto.setPrice(12.5);

    OrderItemDto zeroQty = new OrderItemDto();
    zeroQty.setItemId(102);
    zeroQty.setItemName("book-math");
    zeroQty.setQuantity(0); // should be ignored
    zeroQty.setPrice(10.0);

    OrderDto orderDto = new OrderDto();
    orderDto.setStatus("confirmed");
    orderDto.setTotalPrice(25.0);
    orderDto.setItems(List.of(itemDto, zeroQty));

    Item remoteItem = new Item(101, "Physics", 10, 12.5, "desc");
    mockWebClientGetReturnsItem(remoteItem);
    mockWebClientPutAcceptsInventoryAndReturns(InventoryItemDto.class, new InventoryItemDto());

    ArgumentCaptor<Orders> ordersCaptor = ArgumentCaptor.forClass(Orders.class);
    when(ordersRepository.save(any(Orders.class))).thenAnswer(inv -> inv.getArgument(0));

    // Act
    Orders saved = orderService.createOrder(orderDto);

    // Assert
    assertNotNull(saved);
    assertEquals(25.0, saved.getTotalPrice());
    assertEquals(Status.CONFIRMED, saved.getStatus());
    assertEquals(1, saved.getOrderItems().size(), "Only positive-quantity items should be added");

    verify(ordersRepository).save(ordersCaptor.capture());
    Orders captured = ordersCaptor.getValue();
    assertEquals(1, captured.getOrderItems().size());
    OrderItem oi = captured.getOrderItems().getFirst();
    assertEquals(101, oi.getItemId());
    // The implementation uses the fetched Item name, not the DTO name
    assertEquals("Physics", oi.getItemName());
    assertEquals(2, oi.getQuantity());
  }

  @Test
  @DisplayName("getOrder should return order when found, null otherwise")
  void getOrder_foundAndNotFound() {
    Orders existing = new Orders();
    when(ordersRepository.findById(1)).thenReturn(Optional.of(existing));
    when(ordersRepository.findById(2)).thenReturn(Optional.empty());

    assertSame(existing, orderService.getOrder(1));
    assertNull(orderService.getOrder(2));
  }

  @Test
  @DisplayName("getAllOrders returns repository list or empty list")
  void getAllOrders_variants() {
    Orders a = new Orders();
    when(ordersRepository.findAll()).thenReturn(List.of(a)).thenReturn(List.of());

    List<Orders> first = orderService.getAllOrders();
    assertEquals(1, first.size());

    List<Orders> second = orderService.getAllOrders();
    assertNotNull(second);
    assertTrue(second.isEmpty());
  }

  @Test
  @DisplayName("updateOrder updates fields when present and not cancelled")
  void updateOrder_updatesFields() {
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    when(ordersRepository.findById(5)).thenReturn(Optional.of(existing));
    when(ordersRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    OrderDto dto = new OrderDto();
    dto.setStatus("shipped");
    dto.setTotalPrice(99.9);
    dto.setItems(List.of());

    Orders updated = orderService.updateOrder(5, dto);
    assertNotNull(updated);
    assertEquals(Status.SHIPPED, updated.getStatus());
    assertEquals(99.9, updated.getTotalPrice());
  }

  @Test
  @DisplayName("updateOrder throws when current order is CANCELLED")
  void updateOrder_throwsWhenAlreadyCancelled() {
    Orders existing = new Orders();
    existing.setStatus(Status.CANCELLED);
    when(ordersRepository.findById(6)).thenReturn(Optional.of(existing));

    OrderDto dto = new OrderDto();
    dto.setStatus("confirmed");
    dto.setTotalPrice(10);
    dto.setItems(List.of());

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orderService.updateOrder(6, dto));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
  }

  @Test
  @DisplayName("updateOrder returns null when order not found")
  void updateOrder_returnsNullWhenMissing() {
    when(ordersRepository.findById(7)).thenReturn(Optional.empty());
    OrderDto dto = new OrderDto();
    dto.setStatus("confirmed");
    dto.setTotalPrice(10);
    dto.setItems(List.of());

    assertNull(orderService.updateOrder(7, dto));
  }

  @Test
  @DisplayName("updateOrder restocks items when transitioning to CANCELLED")
  void updateOrder_triggersRestockOnCancel() {
    // existing order with one item
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    OrderItem orderItem = new OrderItem(201, "book-algebra", 3, 15.0);
    existing.addOrderItem(orderItem);

    when(ordersRepository.findById(8)).thenReturn(Optional.of(existing));
    when(ordersRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    // For restock path, GET should return Item and PUT should return updated Item
    Item fetched = new Item(201, "Algebra", 5, 15.0, "desc");
    Item updatedRemote = new Item(201, "Algebra", 8, 15.0, "desc"); // stock + 3
    mockWebClientGetReturnsItem(fetched);
    mockWebClientPutAcceptsInventoryAndReturns(Item.class, updatedRemote);

    OrderDto dto = new OrderDto();
    dto.setStatus("cancelled"); // triggers restock
    dto.setTotalPrice(0.0);
    dto.setItems(List.of());

    Orders updated = orderService.updateOrder(8, dto);
    assertNotNull(updated);
    assertEquals(Status.CANCELLED, updated.getStatus());
    assertEquals(0.0, updated.getTotalPrice());

    // Verify that web client interactions for GET and PUT were invoked
    verify(webClient, atLeastOnce()).get();
    verify(webClient, atLeastOnce()).put();
  }
}

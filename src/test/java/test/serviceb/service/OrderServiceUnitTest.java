package test.serviceb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import test.serviceb.domain.Item;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.InventoryItemDto;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class OrderServiceUnitTest {

  @Mock
  private OrdersRepository ordersRepository;

  @Mock
  private WebClient.Builder webClientBuilder;

  @Mock
  private WebClient webClient;

  // Deeply mocked parts of WebClient chain
  @Mock
  private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

  @Mock
  private WebClient.RequestBodyUriSpec requestBodyUriSpec;

  @Mock
  private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

  @Mock
  private WebClient.ResponseSpec responseSpec;

  private OrderServiceImpl service;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    // Arrange builder to tolerate null baseUrl and return our client
    when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
    when(webClientBuilder.baseUrl(isNull())).thenReturn(webClientBuilder);
    when(webClientBuilder.build()).thenReturn(webClient);

    // Default stubs for WebClient to avoid NPE in tests that don't use it (use doReturn to avoid generics issues)
    doReturn(requestHeadersUriSpec).when(webClient).get();
    doReturn(requestBodyUriSpec).when(webClient).put();

    service = new OrderServiceImpl(ordersRepository, webClientBuilder);
  }

  // Helper: simple OrderDto with no items
  private OrderDto orderDto(double total, String status) {
    OrderDto dto = new OrderDto();
    dto.setTotalPrice(total);
    dto.setStatus(status);
    return dto;
  }

  // Helper: OrderDto with one item
  private OrderDto orderDtoWithItem() {
    OrderDto dto = orderDto(9.5, "confirmed");
    OrderItemDto item = new OrderItemDto();
    item.setItemId(42);
    item.setItemName("Gadget");
    item.setQuantity(2);
    item.setPrice(9.5);
    dto.setItems(List.of(item));
    return dto;
  }

  @Test
  void createOrder_withNoItems_savesOrderAndReturnsSaved() {
    // Arrange
    Orders saved = new Orders();
    saved.setTotalPrice(100.0);
    saved.setStatus(Status.CONFIRMED);
    when(ordersRepository.save(any(Orders.class))).thenReturn(saved);

    // Act
    Orders result = service.createOrder(orderDto(100.0, "confirmed"));

    // Assert
    assertNotNull(result);
    assertEquals(100.0, result.getTotalPrice());
    assertEquals(Status.CONFIRMED, result.getStatus());
    verify(ordersRepository, times(1)).save(any(Orders.class));
    verifyNoMoreInteractions(ordersRepository);
  }

  @Test
  void getOrder_whenPresent_returnsOrder() {
    Orders order = new Orders();
    when(ordersRepository.findById(5)).thenReturn(Optional.of(order));

    Orders result = service.getOrder(5);

    assertSame(order, result);
    verify(ordersRepository).findById(5);
  }

  @Test
  void getOrder_whenMissing_returnsNull() {
    when(ordersRepository.findById(7)).thenReturn(Optional.empty());

    Orders result = service.getOrder(7);

    assertNull(result);
    verify(ordersRepository).findById(7);
  }

  @Test
  void getAllOrders_whenNonEmpty_returnsList() {
    when(ordersRepository.findAll()).thenReturn(List.of(new Orders(), new Orders()));

    List<Orders> result = service.getAllOrders();

    assertEquals(2, result.size());
    verify(ordersRepository).findAll();
  }

  @Test
  void getAllOrders_whenEmpty_returnsEmptyList() {
    when(ordersRepository.findAll()).thenReturn(List.of());

    List<Orders> result = service.getAllOrders();

    assertNotNull(result);
    assertTrue(result.isEmpty());
    verify(ordersRepository).findAll();
  }

  @Test
  void updateOrder_whenPresentAndNotCancelled_updatesAndSaves() {
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    existing.setTotalPrice(10.0);
    when(ordersRepository.findById(1)).thenReturn(Optional.of(existing));

    Orders saved = new Orders();
    saved.setStatus(Status.SHIPPED);
    saved.setTotalPrice(20.0);
    when(ordersRepository.save(any(Orders.class))).thenReturn(saved);

    Orders result = service.updateOrder(1, orderDto(20.0, "shipped"));

    assertNotNull(result);
    assertEquals(Status.SHIPPED, result.getStatus());
    assertEquals(20.0, result.getTotalPrice());

    ArgumentCaptor<Orders> captor = ArgumentCaptor.forClass(Orders.class);
    verify(ordersRepository).save(captor.capture());
    Orders toSave = captor.getValue();
    assertEquals(Status.SHIPPED, toSave.getStatus());
    assertEquals(20.0, toSave.getTotalPrice());
  }

  @Test
  void updateOrder_whenExistingIsCancelled_throwsBadRequest() {
    Orders existing = new Orders();
    existing.setStatus(Status.CANCELLED);
    when(ordersRepository.findById(2)).thenReturn(Optional.of(existing));

    ResponseStatusException ex = assertThrows(ResponseStatusException.class,
        () -> service.updateOrder(2, orderDto(15.0, "confirmed")));
    assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    verify(ordersRepository, never()).save(any());
  }

  @Test
  void updateOrder_whenStatusChangedToCancelled_restockIsTriggered() {
    // Existing order with one item
    Orders existing = new Orders();
    existing.setStatus(Status.CONFIRMED);
    OrderItem oi = new OrderItem(10, "Widget", 3, 5.0);
    existing.addOrderItem(oi);
    when(ordersRepository.findById(3)).thenReturn(Optional.of(existing));

    // Prepare inventory GET for restock
    Item inventoryItem = new Item();
    // set fields on Item via reflection-like setters
    inventoryItem.setId(10);
    inventoryItem.setName("Widget");
    inventoryItem.setPrice(5.0);
    inventoryItem.setStock(7);
    inventoryItem.setDescription("desc");

    // Stubbing chain: GET -> retrieve -> bodyToMono(Item)
    doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(eq("/{id}"), eq(10));
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    when(responseSpec.bodyToMono(Item.class)).thenReturn(Mono.just(inventoryItem));

    // Stubbing chain: PUT -> uri -> bodyValue -> retrieve -> bodyToMono(Void)
    doReturn(requestBodyUriSpec).when(webClient).put();
    doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(eq("/{id}"), eq(10));
    doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

    Orders saved = new Orders();
    saved.setStatus(Status.CANCELLED);
    saved.setTotalPrice(0);
    when(ordersRepository.save(any(Orders.class))).thenReturn(saved);

    Orders result = service.updateOrder(3, orderDto(0, "cancelled"));

    assertNotNull(result);
    assertEquals(Status.CANCELLED, result.getStatus());
    // verify GET and PUT were called (restock)
    verify(webClient, atLeastOnce()).get();
    verify(webClient, atLeastOnce()).put();
  }

  @Test
  void updateOrder_whenNotPresent_returnsNull() {
    when(ordersRepository.findById(9)).thenReturn(Optional.empty());

    Orders result = service.updateOrder(9, orderDto(5.0, "confirmed"));

    assertNull(result);
    verify(ordersRepository, never()).save(any());
  }

  @Test
  void createOrder_withItem_happyPath_addsOrderItemAndSaves() {
    // Mock inventory GET to return an Item
    Item inventoryItem = new Item();
    inventoryItem.setId(42);
    inventoryItem.setName("Gadget");
    inventoryItem.setPrice(9.5);
    inventoryItem.setStock(10);
    inventoryItem.setDescription("desc");

    doReturn(requestHeadersUriSpec).when(webClient).get();
    doReturn(requestHeadersSpec).when(requestHeadersUriSpec)
        .uri(eq("/{id}/itemname/{name}"), eq(42), eq("Gadget"));
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    when(responseSpec.bodyToMono(Item.class)).thenReturn(Mono.just(inventoryItem));

    // Mock inventory PUT to succeed
    doReturn(requestBodyUriSpec).when(webClient).put();
    doReturn(requestBodyUriSpec).when(requestBodyUriSpec).uri(eq("/{id}"), eq(42));
    doReturn(requestHeadersSpec).when(requestBodyUriSpec).bodyValue(any());
    doReturn(responseSpec).when(requestHeadersSpec).retrieve();
    when(responseSpec.bodyToMono(eq(InventoryItemDto.class))).thenReturn(Mono.empty());

    // Repository save echoes back an Orders containing the added item
    when(ordersRepository.save(any(Orders.class))).thenAnswer(invocation -> invocation.getArgument(0));

    OrderDto dto = orderDtoWithItem();

    Orders result = service.createOrder(dto);

    assertNotNull(result);
    assertEquals(1, result.getOrderItems().size());
    OrderItem oi = result.getOrderItems().getFirst();
    assertEquals(42, oi.getItemId());
    assertEquals("Gadget", oi.getItemName());
    assertEquals(2, oi.getQuantity());
  }
}

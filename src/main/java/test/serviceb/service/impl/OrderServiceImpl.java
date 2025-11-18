package test.serviceb.service.impl;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import test.serviceb.domain.Item;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.InventoryItemDto;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.OrderService;

/**
 * The OrderServiceImpl class implements the OrderService interface.
 */
@Service
public class OrderServiceImpl implements OrderService {

  private final WebClient webClient;
  private final OrdersRepository ordersRepo;

  /**
   * Constructs an instance of the OrderServiceImpl class.
   *
   * @param ordersRepo the OrdersRepository instance used to manage orders.
   * @param builder    the WebClient.Builder instance used to configure and build a WebClient for interacting
   *                   with the inventory API.
   */
  public OrderServiceImpl(OrdersRepository ordersRepo, WebClient.Builder builder) {
    this.ordersRepo = ordersRepo;
    this.webClient = builder.baseUrl("http://localhost:8080/api/inventory").build();
  }

  @Override
  public Orders createOrder(OrderDto order) {
    Orders newOrder = new Orders();
    newOrder.setTotalPrice(order.getTotalPrice());
    newOrder.setStatus(getStatusFromOrderDto(order));
    for (OrderItemDto itemDto : order.getItems()) {
      OrderItem orderItem = createOrderItem(itemDto);
      newOrder.addOrderItem(orderItem);
    }
    return ordersRepo.save(newOrder);
  }

  @Override
  public Orders getOrder(int orderId) {
    Optional<Orders> order = ordersRepo.findById(orderId);
    return order.orElse(null);
  }

  @Override
  public List<Orders> getAllOrders() {
    List<Orders> orders = ordersRepo.findAll();
    if (!orders.isEmpty()) {
      return orders;
    }
    return List.of();
  }

  @Override
  public Orders updateOrder(int orderId, OrderDto order) {
    Optional<Orders> orderOptional = ordersRepo.findById(orderId);
    if (orderOptional.isPresent()) {
      Orders orderToUpdate = orderOptional.get();
      if (orderToUpdate.getStatus() == Status.CANCELLED) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot update a cancelled order");
      }
      // Update basic fields
      Status status = getStatusFromOrderDto(order);
      if (status != orderToUpdate.getStatus() && status == Status.CANCELLED) {
        restockOrderItems(orderToUpdate);
      }
      orderToUpdate.setStatus(getStatusFromOrderDto(order));
      orderToUpdate.setTotalPrice(order.getTotalPrice());
      return ordersRepo.save(orderToUpdate);
    }
    return null;
  }


  private OrderItem createOrderItem(OrderItemDto itemDto) {
    try {
      Item item = webClient.get().uri("/{id}/itemname/{name}", itemDto.getItemId(), itemDto.getItemName())
          .retrieve().bodyToMono(Item.class).block();
      if (item == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item with ID " + itemDto.getItemId() + " not found");
      }
      InventoryItemDto inventoryItemDto = new InventoryItemDto();
      inventoryItemDto.setName(item.getName());
      inventoryItemDto.setPrice(item.getPrice());
      if (item.getStock() < itemDto.getQuantity()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Insufficient stock for item with ID " + itemDto.getItemId());
      }
      inventoryItemDto.setStock(item.getStock() - itemDto.getQuantity());
      inventoryItemDto.setDescription(item.getDescription());
      webClient.put().uri("/{id}", itemDto.getItemId()).bodyValue(inventoryItemDto)
          .retrieve().bodyToMono(InventoryItemDto.class).block();
      return new OrderItem(item.getId(), item.getName(), itemDto.getQuantity(), itemDto.getPrice());
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch item with ID: " + itemDto.getItemId(), e);
    }
  }

  /**
   * Restocks the items in an order by updating their stock quantities in the inventory system.
   * This method processes each order item using a reactive pipeline, ensures the item's stock
   * is adjusted based on the quantity in the order, and handles concurrency to avoid overloading
   * the inventory service.
   *
   * @param order the order containing items to be restocked with their respective quantities.
   */
  private void restockOrderItems(Orders order) {
    // Optimize by using a reactive pipeline with limited concurrency instead of per-item blocking calls
    reactor.core.publisher.Flux.fromIterable(order.getOrderItems())
        .flatMap(orderItem ->
            webClient.get().uri("/{id}", orderItem.getItemId())
                .retrieve()
                .bodyToMono(Item.class)
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                    new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item with ID " + orderItem.getItemId() + " not found")))
                .flatMap(item -> {
                  InventoryItemDto inventoryItemDto = new InventoryItemDto();
                  inventoryItemDto.setName(item.getName());
                  inventoryItemDto.setPrice(item.getPrice());
                  inventoryItemDto.setStock(item.getStock() + orderItem.getQuantity());
                  inventoryItemDto.setDescription(item.getDescription());
                  return webClient.put().uri("/{id}", orderItem.getItemId())
                      .bodyValue(inventoryItemDto)
                      .retrieve()
                      .bodyToMono(Void.class);
                }), 4) // limit concurrency to avoid overloading inventory service
        .onErrorMap(e -> new RuntimeException("Failed to restock one or more items", e))
        .blockLast();
  }


  /**
   * Extracts a {@link Status} value from the provided {@link OrderDto}.
   * Converts the status string from the order to uppercase and maps it to the corresponding {@link Status} enum value.
   *
   * @param order the {@link OrderDto} containing the status string to be converted.
   * @return the corresponding {@link Status} value based on the status string in the provided {@link OrderDto}.
   */
  private Status getStatusFromOrderDto(OrderDto order) {
    String status = order.getStatus().toUpperCase(Locale.ROOT);
    return switch (status) {
      case "CANCELLED" -> Status.CANCELLED;
      case "SHIPPED" -> Status.SHIPPED;
      default -> Status.CONFIRMED;
    };
  }
}

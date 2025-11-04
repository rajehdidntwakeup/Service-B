package test.serviceb.service.impl;

import java.util.List;
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
import test.serviceb.repository.OrderItemRepository;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.OrderService;

@Service
public class OrderServiceImpl implements OrderService {

  private final WebClient webClient;
  private OrdersRepository ordersRepository;
  private OrderItemRepository orderItemRepository;

  public OrderServiceImpl(OrdersRepository ordersRepository, OrderItemRepository orderItemRepository,
                          WebClient.Builder builder) {
    this.ordersRepository = ordersRepository;
    this.orderItemRepository = orderItemRepository;
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
    return ordersRepository.save(newOrder);
  }

  @Override
  public Orders getOrder(int orderId) {
    Optional<Orders> order = ordersRepository.findById(orderId);
    return order.orElse(null);
  }

  @Override
  public List<Orders> getAllOrders() {
    List<Orders> orders = ordersRepository.findAll();
    if (!orders.isEmpty()) {
      return orders;
    }
    return List.of();
  }

  @Override
  public Orders updateOrder(int orderId, OrderDto order) {
    Optional<Orders> orderOptional = ordersRepository.findById(orderId);
    if (orderOptional.isPresent()) {
      Orders orderToUpdate = orderOptional.get();

      // Update basic fields
      Status status = getStatusFromOrderDto(order);
      if (status != orderToUpdate.getStatus() && status == Status.CANCELLED) {
        restockOrderItems(orderToUpdate);
      }
      orderToUpdate.setStatus(getStatusFromOrderDto(order));
      orderToUpdate.setTotalPrice(order.getTotalPrice());

      // Replace order items with those from the request
      // Using orphanRemoval on Orders.orderItems, clearing the collection will remove old items
      orderToUpdate.getOrderItems().clear();
      if (order.getItems() != null) {
        for (OrderItemDto itemDto : order.getItems()) {
          OrderItem orderItem = createOrderItem(itemDto);
          orderToUpdate.addOrderItem(orderItem);
        }
      }
      return ordersRepository.save(orderToUpdate);
    }
    return null;
  }


  private OrderItem createOrderItem(OrderItemDto itemDto) {
    try {
      Item item = webClient.get().uri("/{id}", itemDto.getItemId())
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
      return new OrderItem(item.getId(), itemDto.getQuantity(), itemDto.getPrice());
    } catch (Exception e) {
      throw new RuntimeException("Failed to fetch item with ID: " + itemDto.getItemId(), e);
    }
  }

  private void restockOrderItems(Orders order) {
    for (OrderItem orderItem : order.getOrderItems()) {
      try {
        Item item = webClient.get().uri("/{id}", orderItem.getItemId())
            .retrieve().bodyToMono(Item.class).block();
        if (item != null) {
          InventoryItemDto inventoryItemDto = new InventoryItemDto();
          inventoryItemDto.setName(item.getName());
          inventoryItemDto.setPrice(item.getPrice());
          inventoryItemDto.setStock(item.getStock() + orderItem.getQuantity());
          inventoryItemDto.setDescription(item.getDescription());
          webClient.put().uri("/{id}", orderItem.getItemId()).bodyValue(inventoryItemDto)
              .retrieve().bodyToMono(InventoryItemDto.class).block();
        }
      } catch (Exception e) {
        throw new RuntimeException("Failed to restock item with ID: " + orderItem.getItemId(), e);
      }
    }
  }

  private Status getStatusFromOrderDto(OrderDto order) {
    return switch (order.getStatus().toUpperCase()) {
      case "CANCELLED" -> Status.CANCELLED;
      case "SHIPPED" -> Status.SHIPPED;
      default -> Status.CONFIRMED;
    };
  }
}

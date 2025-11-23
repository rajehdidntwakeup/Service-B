package test.serviceb.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import test.serviceb.domain.Item;
import test.serviceb.domain.OrderItem;
import test.serviceb.domain.Orders;
import test.serviceb.domain.Status;
import test.serviceb.domain.dto.ExternalInventory;
import test.serviceb.domain.dto.InventoryItemDto;
import test.serviceb.domain.dto.OrderDto;
import test.serviceb.domain.dto.OrderItemDto;
import test.serviceb.repository.OrdersRepository;
import test.serviceb.service.OrderService;
import test.serviceb.service.converter.ConversionProperties;

/**
 * The OrderServiceImpl class implements the OrderService interface.
 */
@Service
public class OrderServiceImpl implements OrderService {

  private final Map<String, WebClient> webClientMap = new HashMap<>();
  private final OrdersRepository ordersRepo;
  private static final String ITEM_PATH = "/{id}/itemname/{name}";


  /**
   * Constructs an instance of OrderServiceImpl.
   *
   * @param ordersRepo the repository used for managing orders
   * @param properties the configuration properties containing external inventory details
   * @param builder    the WebClient.Builder for building web clients for external services
   */
  public OrderServiceImpl(OrdersRepository ordersRepo, ConversionProperties properties, WebClient.Builder builder) {
    this.ordersRepo = ordersRepo;
    for (Map.Entry<String, ExternalInventory> entry : properties.getExternalInventory().entrySet()) {
      ExternalInventory externalService = entry.getValue();
      webClientMap.put(externalService.getName(), builder.baseUrl(externalService.getUrl()).build());
    }
  }

  @Override
  public Orders createOrder(OrderDto order) {
    Orders newOrder = new Orders();
    newOrder.setTotalPrice(order.getTotalPrice());
    newOrder.setStatus(getStatusFromOrderDto(order));
    for (OrderItemDto itemDto : order.getItems()) {
      if (itemDto.getQuantity() > 0) {
        OrderItem orderItem = createOrderItem(itemDto);
        newOrder.addOrderItem(orderItem);
      }
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


  /**
   * Creates an OrderItem instance based on the provided OrderItemDto object.
   * This method communicates with an external service to retrieve and update inventory details.
   * If the item is not found or if the requested quantity is invalid, appropriate exceptions are thrown.
   *
   * @param itemDto the data transfer object containing details of the item to be ordered, such as ID, name, quantity, and price
   * @return a newly created OrderItem object containing the data for the ordered item
   * @throws ResponseStatusException if the item is not found or insufficient stock is available
   * @throws RuntimeException        if there is any failure during the fetching or updating process
   */
  private OrderItem createOrderItem(OrderItemDto itemDto) {
    try {
      WebClient webClient = getWebClient(itemDto.getItemName());
      Item item = webClient.get().uri(ITEM_PATH, itemDto.getItemId(), itemDto.getItemName())
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
      webClient.put().uri(ITEM_PATH, itemDto.getItemId(), itemDto.getItemName())
          .bodyValue(inventoryItemDto)
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
    // Reuse a single DTO instance to comply with PMD's AvoidInstantiatingObjectsInLoops rule
    InventoryItemDto inventoryItemDto = new InventoryItemDto();
    for (OrderItem orderItem : order.getOrderItems()) {
      WebClient webClient = getWebClient(orderItem.getItemName());
      Item item = webClient.get().uri(ITEM_PATH, orderItem.getItemId(), orderItem.getItemName())
          .retrieve().bodyToMono(Item.class).block();
      assertItemFound(item, orderItem.getItemName());
      inventoryItemDto.setName(item.getName());
      inventoryItemDto.setPrice(item.getPrice());
      inventoryItemDto.setStock(item.getStock() + orderItem.getQuantity());
      inventoryItemDto.setDescription(item.getDescription());
      Item updatedItem = webClient.put().uri(ITEM_PATH, orderItem.getItemId(), orderItem.getItemName())
          .bodyValue(inventoryItemDto)
          .retrieve().bodyToMono(Item.class).block();
      assertItemUpdated(updatedItem, orderItem.getItemName());
    }
  }

  /**
   * Ensures that the specified item is found.
   * If the item is null, throws a ResponseStatusException with a NOT_FOUND status.
   *
   * @param item     the item to check for existence
   * @param itemName the name of the item, used in the exception message if the item is not found
   */
  private void assertItemFound(Item item, String itemName) {
    if (item == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item " + itemName + " was not found!");
    }
  }

  /**
   * Verifies that the provided item has been updated. Throws a {@link ResponseStatusException}
   * with a 404 status if the item is null.
   *
   * @param updatedItem the item that is expected to be updated
   * @param itemName    the name of the item being verified, used in the exception message
   */
  private void assertItemUpdated(Item updatedItem, String itemName) {
    if (updatedItem == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Item " + itemName + " was not updated!");
    }
  }


  /**
   * Retrieves a {@link WebClient} instance based on the provided item name.
   * The item name is matched against keys in the internal web client map to find the corresponding {@link WebClient}.
   * If no match is found, a {@link ResponseStatusException} with a status of {@code HttpStatus.NOT_FOUND} is thrown.
   *
   * @param itemName the name of the item used to identify the corresponding {@link WebClient}.
   * @return the {@link WebClient} instance associated with the matching key from the internal web client map.
   * @throws ResponseStatusException if no {@link WebClient} is found for the provided item name.
   */
  private WebClient getWebClient(String itemName) {
    WebClient webClient = null;
    for (String key : webClientMap.keySet()) {
      if (itemName.contains(key)) {
        webClient = webClientMap.get(key);
        break;
      }
    }
    if (webClient == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Item with name " + itemName + " not found");
    }
    return webClient;
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

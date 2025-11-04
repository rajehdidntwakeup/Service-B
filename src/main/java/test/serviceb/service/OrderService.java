package test.serviceb.service;

import java.util.List;

import test.serviceb.domain.Orders;
import test.serviceb.domain.dto.OrderDto;

public interface OrderService {

  Orders createOrder(OrderDto order);

  Orders getOrder(int orderId);

  List<Orders> getAllOrders();

  Orders updateOrder(int orderId, OrderDto order);
}

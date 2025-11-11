package test.serviceb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import test.serviceb.domain.OrderItem;

/**
 * The OrderItemRepository class is a JPA repository responsible for managing order items.
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}

package test.serviceb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import test.serviceb.domain.Orders;

@Repository
public interface OrdersRepository extends JpaRepository<Orders, Integer> {
}

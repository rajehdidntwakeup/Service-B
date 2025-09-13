package test.serviceb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import test.serviceb.domain.Selling;

@Repository
public interface SellingRepository extends JpaRepository<Selling, Integer> {
}

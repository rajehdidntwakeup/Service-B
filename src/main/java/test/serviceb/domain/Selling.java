package test.serviceb.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;

@Entity
@Data
@AllArgsConstructor
public class Selling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int sellingId;
    private int petId;
    private String customerName;
    private int price;

    public Selling() {
    }

    public Selling(int petId, String customerName, int price) {
        this.petId = petId;
        this.customerName = customerName;
        this.price = price;
    }
}

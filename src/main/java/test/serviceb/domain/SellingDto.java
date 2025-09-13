package test.serviceb.domain;

import lombok.Data;

@Data
public class SellingDto {

    private int petId;
    private String customerName;
    private int price;

    public SellingDto() {
    }

    public SellingDto(int petId, String customerName, int price) {
        this.petId = petId;
        this.customerName = customerName;
        this.price = price;
    }
}

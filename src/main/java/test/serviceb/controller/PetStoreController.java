package test.serviceb.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import test.serviceb.domain.Selling;
import test.serviceb.domain.SellingDto;
import test.serviceb.repository.SellingRepository;

import java.util.List;

@RestController()
@RequestMapping("/store")
@CrossOrigin(origins = "http://localhost:8080", allowedHeaders = "*")
public class PetStoreController {

    private final WebClient webClient;

    private final SellingRepository sellingRepository;

    public PetStoreController(WebClient.Builder builder, SellingRepository sellingRepository) {
        this.webClient = builder.baseUrl("http://localhost:8081/cats").build();
        this.sellingRepository = sellingRepository;
    }

    @PostMapping("/make-a-sell")
    public ResponseEntity<String> makeASell(@RequestBody SellingDto sellingDto) {
        try {
            sellingRepository.save(new Selling(sellingDto.getPetId(), sellingDto.getCustomerName(), sellingDto.getPrice()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.ok("Selling is done");
    }

    @GetMapping("/all-deals")
    public ResponseEntity<List<Selling>> getSelling() {
        List<Selling> sellingList = sellingRepository.findAll();
        return ResponseEntity.ok(sellingList);
    }

    @GetMapping("/pets")
    public ResponseEntity<List<?>> getPets() {
        try {
            List<?> petList = webClient.get().uri("/all").retrieve().bodyToMono(List.class).block();
            return ResponseEntity.ok(petList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}




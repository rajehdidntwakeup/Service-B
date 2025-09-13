package test.serviceb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import test.serviceb.controller.PetStoreController;
import test.serviceb.domain.Selling;
import test.serviceb.domain.SellingDto;
import test.serviceb.repository.SellingRepository;

public class serviceTest {

    @Mock
    public SellingRepository sellingRepository;

    @Mock
    public WebClient.Builder webClient;

    @InjectMocks
    public PetStoreController controller = new PetStoreController(webClient, sellingRepository);



    @Test
    public void test() {
        SellingDto sellingDto = new SellingDto(1, "Rajehdidntwakeup", 100);
        Mockito
                .when(sellingRepository
                        .save(new Selling(1, "Rajehdidntwakeup", 100)))
                .thenReturn(null);

        Assertions.assertDoesNotThrow(() -> controller.makeASell(sellingDto));
    }
}

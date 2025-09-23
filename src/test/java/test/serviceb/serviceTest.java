package test.serviceb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import test.serviceb.controller.PetStoreController;
import test.serviceb.domain.Selling;
import test.serviceb.domain.SellingDto;
import test.serviceb.repository.SellingRepository;

@ExtendWith(MockitoExtension.class)
public class serviceTest {

    @Mock
    private SellingRepository sellingRepository;

    @Mock(answer = Answers.RETURNS_SELF)
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private PetStoreController controller;

    @Test
    public void testMakeASell() {
        // Arrange - create mock behaviors and expected objects
        SellingDto sellingDto = new SellingDto(1, "Rajehdidntwakeup", 100);
        Selling sellingEntity = new Selling(1, "Rajehdidntwakeup", 100);

        Mockito
                .when(sellingRepository.save(ArgumentMatchers.any(Selling.class)))
                .thenReturn(sellingEntity);

        // Act - invoke the method under test
        Assertions.assertDoesNotThrow(() -> controller.makeASell(sellingDto));

        // Verify - ensure proper interactions with mocks
        Mockito.verify(sellingRepository, Mockito.times(1)).save(ArgumentMatchers.any(Selling.class));
    }
}

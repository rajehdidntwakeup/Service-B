package test.serviceb.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GlobalExceptionHandlerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  @DisplayName("POST /api/order with invalid OrderDto should return 400 with structured validation errors from GlobalExceptionHandler")
  void shouldReturnBadRequestAndValidationErrorStructure_WhenOrderDtoInvalid() throws Exception {
    // Given: an invalid OrderDto payload
    String invalidOrderJson = """
        {
          "totalPrice": 0,
          "status": "",
          "items": []
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidOrderJson))
        .andExpect(status().isBadRequest())
        // Global structure asserted
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        // Field errors asserted (messages come from annotations)
        .andExpect(jsonPath("$.errors.totalPrice").value("Total price must be provided & non-negative"))
        .andExpect(jsonPath("$.errors.status").value("Status must be provided"))
        .andExpect(jsonPath("$.errors.items").value("At least one item is required"));
  }

  @Test
  @DisplayName("POST /api/order with invalid OrderItemDto should return 400 and include nested field errors")
  void shouldReturnBadRequestAndNestedFieldErrors_WhenOrderItemsInvalid() throws Exception {
    // Given: top-level valid fields but invalid nested item fields
    String invalidItemsJson = """
        {
          "totalPrice": 10.5,
          "status": "NEW",
          "items": [
            {
              "itemId": 0,
              "itemName": "",
              "price": -1.0,
              "quantity": 0
            }
          ]
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/order")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidItemsJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        // Nested field errors (keys are taken from FieldError#getField, including indexes)
        .andExpect(jsonPath("$.errors['items[0].itemId']").value("Item ID must be provided & non-negative"))
        .andExpect(jsonPath("$.errors['items[0].itemName']").value("Item name must be provided"))
        .andExpect(jsonPath("$.errors['items[0].price']").value("Price must be provided & non-negative"))
        .andExpect(jsonPath("$.errors['items[0].quantity']").value("Quantity must be provided & non-negative"));
  }

  @Test
  @DisplayName("PUT /api/order/{id} with invalid OrderDto should return 400 with structured validation errors from GlobalExceptionHandler")
  void shouldReturnBadRequestAndValidationErrorStructure_WhenUpdatingOrderWithInvalidOrderDto() throws Exception {
    // Given: an invalid OrderDto payload for update
    String invalidOrderJson = """
        {
          "totalPrice": 0,
          "status": "",
          "items": []
        }
        """;

    // When & Then
    mockMvc.perform(put("/api/order/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidOrderJson))
        .andExpect(status().isBadRequest())
        // Global structure asserted
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        // Field errors asserted (messages come from annotations)
        .andExpect(jsonPath("$.errors.totalPrice").value("Total price must be provided & non-negative"))
        .andExpect(jsonPath("$.errors.status").value("Status must be provided"))
        .andExpect(jsonPath("$.errors.items").value("At least one item is required"));
  }

  @Test
  @DisplayName("PUT /api/order/{id} with invalid OrderItemDto should return 400 and include nested field errors")
  void shouldReturnBadRequestAndNestedFieldErrors_WhenUpdatingOrderWithInvalidItems() throws Exception {
    // Given: top-level valid fields but invalid nested item fields for update
    String invalidItemsJson = """
        {
          "totalPrice": 10.5,
          "status": "NEW",
          "items": [
            {
              "itemId": 0,
              "itemName": "",
              "price": -1.0,
              "quantity": 0
            }
          ]
        }
        """;

    // When & Then
    mockMvc.perform(put("/api/order/{id}", 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidItemsJson))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("Validation failed"))
        // Nested field errors (keys are taken from FieldError#getField, including indexes)
        .andExpect(jsonPath("$.errors['items[0].itemId']").value("Item ID must be provided & non-negative"))
        .andExpect(jsonPath("$.errors['items[0].itemName']").value("Item name must be provided"))
        .andExpect(jsonPath("$.errors['items[0].price']").value("Price must be provided & non-negative"))
        .andExpect(jsonPath("$.errors['items[0].quantity']").value("Quantity must be provided & non-negative"));
  }
}

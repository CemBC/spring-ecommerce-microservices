package com.ecommerce.inventory_service.controller;

import com.ecommerce.inventory_service.dto.CreateInventoryRequest;
import com.ecommerce.inventory_service.dto.InventoryResponse;
import com.ecommerce.inventory_service.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void shouldCreateInventory() throws Exception {
        CreateInventoryRequest request = new CreateInventoryRequest(1L, 100);
        InventoryResponse response = new InventoryResponse(
                1L, 1L, 100, 0, 100, 0L, LocalDateTime.now(), LocalDateTime.now()
        );
        when(inventoryService.create(any(CreateInventoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.availableQuantity").value(100));
    }

    @Test
    void shouldRejectNegativeQuantity() throws Exception {
        String body = """
                {"productId":1,"quantity":-1}
                """;

        mockMvc.perform(post("/api/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}

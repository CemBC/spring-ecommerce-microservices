package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.client.InventoryClient;
import com.ecommerce.order_service.client.ProductClient;
import com.ecommerce.order_service.client.dto.ProductSnapshotResponse;
import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class OrderFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("order_flow_test_db")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void properties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private ProductClient productClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        when(
                productClient.getProduct(
                        anyLong()
                )
        ).thenAnswer(invocation -> {
            Long productId =
                    invocation.getArgument(0);

            return new ProductSnapshotResponse(
                    productId,
                    "Test Product " + productId,
                    "description",
                    new BigDecimal("100.00"),
                    "SKU-" + productId,
                    true,
                    1L,
                    "Category",
                    null,
                    null
            );
        });

        doNothing()
                .when(inventoryClient)
                .reserveOrder(
                        anyLong(),
                        anyList()
                );

        doNothing()
                .when(inventoryClient)
                .releaseOrder(
                        anyLong()
                );

        doNothing()
                .when(inventoryClient)
                .confirmOrder(
                        anyLong()
                );
    }

    @Test
    void shouldCreateConfirmCompleteAndRejectCancellation()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": 7,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "quantity": 2
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                )
                .andExpect(
                        jsonPath("$.totalAmount")
                                .value(200.00)
                );

        Order order =
                orderRepository
                        .findAll()
                        .getFirst();

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/confirm",
                                order.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                );

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/complete",
                                order.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("COMPLETED")
                );

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/cancel",
                                order.getId()
                        )
                )
                .andExpect(
                        status().isConflict()
                );
    }

    @Test
    void shouldRejectDuplicateProductIdsInSameOrder()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": 7,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "quantity": 1
                                            },
                                            {
                                              "productId": 1,
                                              "quantity": 2
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(
                        status().isBadRequest()
                );
    }

    @Test
    void shouldFilterByUserAndStatus()
            throws Exception {

        createOrder(10L);
        createOrder(20L);

        Order user10Order =
                orderRepository
                        .findAll()
                        .stream()
                        .filter(order ->
                                order.getUserId()
                                        .equals(10L)
                        )
                        .findFirst()
                        .orElseThrow();

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/confirm",
                                user10Order.getId()
                        )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get("/api/orders")
                                .param(
                                        "userId",
                                        "10"
                                )
                                .param(
                                        "status",
                                        "CONFIRMED"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].userId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("CONFIRMED")
                );
    }

    private void createOrder(
            Long userId
    ) throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "userId": %d,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "quantity": 1
                                            }
                                          ]
                                        }
                                        """.formatted(
                                        userId
                                ))
                )
                .andExpect(
                        status().isCreated()
                );
    }
}

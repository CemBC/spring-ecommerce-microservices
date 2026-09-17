package com.ecommerce.order_service.integration;

import com.ecommerce.order_service.entity.Order;
import com.ecommerce.order_service.entity.OrderStatus;
import com.ecommerce.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

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

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateConfirmCompleteAndRejectCancellation()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "userId": 7,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "productName": "MacBook Air M3",
                                              "quantity": 2,
                                              "unitPrice": 45000
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(90000.00));

        Order order = orderRepository
                .findAll()
                .getFirst();

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/confirm",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/complete",
                                order.getId()
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/cancel",
                                order.getId()
                        )
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectDuplicateProductIdsInSameOrder()
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "userId": 7,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "productName": "MacBook Air M3",
                                              "quantity": 1,
                                              "unitPrice": 45000
                                            },
                                            {
                                              "productId": 1,
                                              "productName": "MacBook Air M3",
                                              "quantity": 2,
                                              "unitPrice": 45000
                                            }
                                          ]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Duplicate productId in order items: 1"
                                )
                );
    }

    @Test
    void shouldFilterByUserAndStatus()
            throws Exception {

        createOrder(10L);
        createOrder(20L);

        Order user10Order = orderRepository
                .findAll()
                .stream()
                .filter(order -> order.getUserId().equals(10L))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(
                        patch(
                                "/api/orders/{id}/confirm",
                                user10Order.getId()
                        )
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/orders")
                                .param("userId", "10")
                                .param("status", "CONFIRMED")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].userId").value(10))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    private void createOrder(Long userId)
            throws Exception {

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "userId": %d,
                                          "items": [
                                            {
                                              "productId": 1,
                                              "productName": "Test Product",
                                              "quantity": 1,
                                              "unitPrice": 100
                                            }
                                          ]
                                        }
                                        """.formatted(userId))
                )
                .andExpect(status().isCreated());
    }
}

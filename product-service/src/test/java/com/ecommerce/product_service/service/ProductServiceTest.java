package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.CreateProductRequest;
import com.ecommerce.product_service.dto.ProductResponse;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.entity.Product;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.mapper.ProductMapper;
import com.ecommerce.product_service.repository.CategoryRepository;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private ProductMapper productMapper;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();

        productService = new ProductService(
                productRepository,
                categoryRepository,
                productMapper
        );
    }

    @Test
    void shouldCreateProduct() {
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        CreateProductRequest request = new CreateProductRequest(
                "MacBook Air M3",
                "Laptop",
                new BigDecimal("45000.00"),
                "MBA-M3",
                1L
        );

        when(productRepository.existsBySku("MBA-M3"))
                .thenReturn(false);

        when(categoryRepository.findById(1L))
                .thenReturn(Optional.of(category));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(1L);
                    return product;
                });

        ProductResponse response = productService.create(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("MacBook Air M3", response.name());
        assertEquals("MBA-M3", response.sku());
        assertEquals("Electronics", response.categoryName());

        verify(productRepository).save(any(Product.class));
    }

    @Test
    void shouldThrowConflictWhenSkuAlreadyExists() {
        CreateProductRequest request = new CreateProductRequest(
                "MacBook Air M3",
                "Laptop",
                new BigDecimal("45000.00"),
                "MBA-M3",
                1L
        );

        when(productRepository.existsBySku("MBA-M3"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> productService.create(request)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        CreateProductRequest request = new CreateProductRequest(
                "MacBook Air M3",
                "Laptop",
                new BigDecimal("45000.00"),
                "MBA-M3",
                999L
        );

        when(productRepository.existsBySku("MBA-M3"))
                .thenReturn(false);

        when(categoryRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.create(request)
        );

        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldReturnProductById() {
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        Product product = Product.builder()
                .id(1L)
                .name("MacBook Air M3")
                .description("Laptop")
                .price(new BigDecimal("45000.00"))
                .sku("MBA-M3")
                .active(true)
                .category(category)
                .build();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1L);

        assertEquals(1L, response.id());
        assertEquals("MacBook Air M3", response.name());
    }

    @Test
    void shouldThrowWhenProductDoesNotExist() {
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getById(999L)
        );
    }
}
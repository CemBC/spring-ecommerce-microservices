package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.CategoryResponse;
import com.ecommerce.product_service.dto.CreateCategoryRequest;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.mapper.CategoryMapper;
import com.ecommerce.product_service.repository.CategoryRepository;
import com.ecommerce.product_service.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private CategoryMapper categoryMapper;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        productRepository = mock(ProductRepository.class);
        categoryMapper = new CategoryMapper();

        categoryService = new CategoryService(
                categoryRepository,
                categoryMapper,
                productRepository
        );
    }

    @Test
    void shouldCreateCategory() {
        CreateCategoryRequest request =
                new CreateCategoryRequest(
                        "Electronics",
                        "Electronic devices"
                );

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(false);

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category category = invocation.getArgument(0);
                    category.setId(1L);
                    return category;
                });

        CategoryResponse response =
                categoryService.create(request);

        assertEquals(1L, response.id());
        assertEquals("Electronics", response.name());
    }

    @Test
    void shouldThrowConflictWhenCategoryExists() {
        CreateCategoryRequest request =
                new CreateCategoryRequest(
                        "Electronics",
                        "Electronic devices"
                );

        when(categoryRepository.existsByName("Electronics"))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> categoryService.create(request)
        );

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldPreventDeletingCategoryWithProducts() {
        Category category = Category.builder()
                .id(1L)
                .name("Electronics")
                .build();

        when(categoryRepository.findById(1L))
                .thenReturn(java.util.Optional.of(category));

        when(productRepository.existsByCategoryId(1L))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> categoryService.delete(1L)
        );

        verify(categoryRepository, never()).delete(any());
    }
}
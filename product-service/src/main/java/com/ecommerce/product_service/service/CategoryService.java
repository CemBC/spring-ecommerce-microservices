package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.CategoryResponse;
import com.ecommerce.product_service.dto.CreateCategoryRequest;
import com.ecommerce.product_service.dto.UpdateCategoryRequest;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.mapper.CategoryMapper;
import com.ecommerce.product_service.repository.CategoryRepository;
import com.ecommerce.product_service.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final ProductRepository productRepository;
    public CategoryService(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper, ProductRepository productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAll(
            String name,
            Pageable pageable
    ) {
        Page<Category> categories;

        if (name == null || name.isBlank()) {
            categories = categoryRepository.findAll(pageable);
        } else {
            categories =
                    categoryRepository.findByNameContainingIgnoreCase(
                            name,
                            pageable
                    );
        }

        return categories.map(categoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = findCategory(id);
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {

        if (categoryRepository.existsByName(request.name())) {
            throw new ConflictException(
                    "Category already exists: " + request.name()
            );
        }

        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .build();

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Transactional
    public CategoryResponse update(
            Long id,
            UpdateCategoryRequest request
    ) {
        Category category = findCategory(id);

        if (!category.getName().equals(request.name())
                && categoryRepository.existsByName(request.name())) {

            throw new ConflictException(
                    "Category already exists: " + request.name()
            );
        }

        category.setName(request.name());
        category.setDescription(request.description());

        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = findCategory(id);

        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException(
                    "Category cannot be deleted because it contains products"
            );
        }

        categoryRepository.delete(category);
    }


    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->new ResourceNotFoundException("Category not found with id: " + id)
                );
    }
}
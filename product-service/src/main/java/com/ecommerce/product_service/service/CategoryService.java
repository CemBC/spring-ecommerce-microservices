package com.ecommerce.product_service.service;

import com.ecommerce.product_service.dto.CategoryResponse;
import com.ecommerce.product_service.dto.CreateCategoryRequest;
import com.ecommerce.product_service.dto.UpdateCategoryRequest;
import com.ecommerce.product_service.entity.Category;
import com.ecommerce.product_service.exception.ConflictException;
import com.ecommerce.product_service.exception.ResourceNotFoundException;
import com.ecommerce.product_service.mapper.CategoryMapper;
import com.ecommerce.product_service.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(
            CategoryRepository categoryRepository,
            CategoryMapper categoryMapper
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
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

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->new ResourceNotFoundException("Category not found with id: " + id)
                );
    }
}
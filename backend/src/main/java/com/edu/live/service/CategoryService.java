package com.edu.live.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edu.live.dto.CategoryRequest;
import com.edu.live.entity.Category;

import java.util.List;

public interface CategoryService {
    List<Category> listAll();

    Category create(CategoryRequest request);

    Category update(Long id, CategoryRequest request);

    void delete(Long id);
}

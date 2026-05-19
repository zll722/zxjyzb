package com.edu.live.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edu.live.common.BusinessException;
import com.edu.live.dto.CategoryRequest;
import com.edu.live.entity.Category;
import com.edu.live.mapper.CategoryMapper;
import com.edu.live.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryMapper categoryMapper;

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort).orderByDesc(Category::getId));
    }

    @Override
    public Category create(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setSort(request.getSort());
        categoryMapper.insert(category);
        return category;
    }

    @Override
    public Category update(Long id, CategoryRequest request) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException(404, "分类不存在");
        }
        category.setName(request.getName());
        category.setSort(request.getSort());
        categoryMapper.updateById(category);
        return category;
    }

    @Override
    public void delete(Long id) {
        categoryMapper.deleteById(id);
    }
}

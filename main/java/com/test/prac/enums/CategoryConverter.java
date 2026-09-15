package com.test.prac.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CategoryConverter implements AttributeConverter<Category, Integer> {

	@Override
	public Integer convertToDatabaseColumn(Category category) {

		return category == null ? null : category.getCategory();
	}

	@Override
	public Category convertToEntityAttribute(Integer category) {

		return category == null ? null : Category.boardCategory(category);
	}
}

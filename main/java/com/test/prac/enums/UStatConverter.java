package com.test.prac.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class UStatConverter implements AttributeConverter<UStat, String> {

	@Override
	public String convertToDatabaseColumn(UStat ustat) {
		return ustat == null ? null : ustat.getStatus();
	}

	@Override
	public UStat convertToEntityAttribute(String ustat) {
		return ustat == null ? null : UStat.userStatus(ustat);
	}
}

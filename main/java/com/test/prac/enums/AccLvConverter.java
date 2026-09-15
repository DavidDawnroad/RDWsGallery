package com.test.prac.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

//CHAR(1) <- AttributeConverter -> Enum 매핑을 위한 Converter 정의 파일
@Converter(autoApply = true)
public class AccLvConverter implements AttributeConverter<AccLv, String> {

	@Override
	public String convertToDatabaseColumn(AccLv acclv) {
		return acclv == null ? null : acclv.getLevel();
	}

	@Override
	public AccLv convertToEntityAttribute(String acclv) {
		return acclv == null ? null : AccLv.accessLevel(acclv);
	}
}
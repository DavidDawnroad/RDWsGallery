package com.test.prac.enums;

import java.util.Arrays;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UStat {
	NORMAL("N", "정상"),
	DORMANT("D", "휴면"),
	LOCKED("L", "잠김"),
	BANNED("B", "차단"),
	TERMINATED("T", "탈퇴");

	private final String status;
	private final String description;

	public static UStat userStatus(String stat) {

		return Arrays.stream(values())
				.filter(s -> s.status.equals(stat))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown User Status: " + stat));
	}
}

package com.test.prac.exception;

public class NicknameNotChangedException extends RuntimeException {
	public NicknameNotChangedException(String message) {
		super(message);
	}
}
package com.test.prac.exception;

public class PasswordNotChangedException extends RuntimeException {
	public PasswordNotChangedException(String message) {
		super(message);
	}
}

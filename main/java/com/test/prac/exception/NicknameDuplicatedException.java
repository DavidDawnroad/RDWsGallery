package com.test.prac.exception;

public class NicknameDuplicatedException extends RuntimeException {
	public NicknameDuplicatedException(String message) {
		super(message);
	}
}
package com.aug3.storage.mongoclient.exception;

public class BadConfigException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public BadConfigException() {
		super();
	}

	public BadConfigException(String msg) {
		super(msg);
	}

}

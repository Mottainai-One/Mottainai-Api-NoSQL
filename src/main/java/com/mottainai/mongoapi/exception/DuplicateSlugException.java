package com.mottainai.mongoapi.exception;

public class DuplicateSlugException extends RuntimeException {

    public DuplicateSlugException(String message) {
        super(message);
    }
}

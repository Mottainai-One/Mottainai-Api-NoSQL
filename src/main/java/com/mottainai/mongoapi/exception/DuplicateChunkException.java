package com.mottainai.mongoapi.exception;

public class DuplicateChunkException extends RuntimeException {

    public DuplicateChunkException(String message) {
        super(message);
    }
}

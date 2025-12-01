package com.ptit.schedule.exception;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("Đã tồn tại %s với %s = '%s'", resourceName, fieldName, fieldValue));
    }
}
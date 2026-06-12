package com.antss_prescription.exception;

import java.util.UUID;

class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
    }
}

package com.traintrack.coreapi.common.exception;

/** Deliberately the same message/status whether the email is unknown or the password is wrong. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}

package com.waytta;

public class SaltException extends Exception {

    public SaltException(String message) {
        super(message, null, true, false);
    }

    public SaltException(String message, Throwable cause) {
        super(message, cause);
    }

    public SaltException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}

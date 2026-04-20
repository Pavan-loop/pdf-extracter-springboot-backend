package com.madara.security.Exception.type;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException() {
        super("Session/s not found for this user");
    }
}

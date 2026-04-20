package com.madara.security.model;

public enum Role {
    USER,
    ADMIN;

    public String getAuthority(){
        return "ROLE_" + this.name();
    }
}

package com.legalpay.api.dto;

public class AuthRequest {
    private String email;
    private String password;

    public AuthRequest() {}

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public static AuthRequestBuilder builder() {
        return new AuthRequestBuilder();
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public static class AuthRequestBuilder {
        private String email;
        private String password;

        public AuthRequestBuilder email(String email) { this.email = email; return this; }
        public AuthRequestBuilder password(String password) { this.password = password; return this; }
        public AuthRequest build() { return new AuthRequest(email, password); }
    }
}

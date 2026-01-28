package com.legalpay.api.dto;

public class AuthResponse {
    private String token;
    private String role;
    private String username;
    private String name;
    private String id;

    public AuthResponse() {}

    public AuthResponse(String token, String role, String username, String name, String id) {
        this.token = token;
        this.role = role;
        this.username = username;
        this.name = name;
        this.id = id;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public static class AuthResponseBuilder {
        private String token;
        private String role;
        private String username;
        private String name;
        private String id;

        public AuthResponseBuilder token(String token) { this.token = token; return this; }
        public AuthResponseBuilder role(String role) { this.role = role; return this; }
        public AuthResponseBuilder username(String username) { this.username = username; return this; }
        public AuthResponseBuilder name(String name) { this.name = name; return this; }
        public AuthResponseBuilder id(String id) { this.id = id; return this; }
        public AuthResponse build() { return new AuthResponse(token, role, username, name, id); }
    }
}

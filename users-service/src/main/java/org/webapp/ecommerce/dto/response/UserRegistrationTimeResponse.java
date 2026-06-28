package org.webapp.ecommerce.dto.response;

import java.time.LocalDateTime;

public class UserRegistrationTimeResponse {

    private String username;

    private LocalDateTime createdAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

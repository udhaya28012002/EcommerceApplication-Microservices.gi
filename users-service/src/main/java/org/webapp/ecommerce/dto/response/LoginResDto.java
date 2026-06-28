package org.webapp.ecommerce.dto.response;

public class LoginResDto {

    private String context;

    public LoginResDto(String context) {
        this.context = context;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}

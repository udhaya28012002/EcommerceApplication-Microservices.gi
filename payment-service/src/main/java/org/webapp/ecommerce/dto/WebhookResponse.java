package org.webapp.ecommerce.dto;

public class WebhookResponse {
    private String message;
    private String eventType;

    public WebhookResponse(String message, String eventType) {
        this.message = message;
        this.eventType = eventType;
    }

    public String getMessage() { return message; }
    public String getEventType() { return eventType; }
}
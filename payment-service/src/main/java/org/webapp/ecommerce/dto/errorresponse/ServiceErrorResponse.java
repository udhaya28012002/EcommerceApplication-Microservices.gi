package org.webapp.ecommerce.dto.errorresponse;

import java.time.LocalDateTime;

public class ServiceErrorResponse {

    private String errorCode;
    private String message;
    private LocalDateTime timeStamp;

    public ServiceErrorResponse(String errorCode, LocalDateTime timeStamp, String message) {
        this.errorCode = errorCode;
        this.timeStamp = timeStamp;
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimeStamp() {
        return timeStamp;
    }

}

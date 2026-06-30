package org.webapp.ecommerce.dto.errorresponse;

public class ServiceErrorResponse {

    private int errorCode;
    private String message;
    private String timeStamp;

    public ServiceErrorResponse(int errorCode, String timeStamp, String message) {
        this.errorCode = errorCode;
        this.timeStamp = timeStamp;
        this.message = message;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

}

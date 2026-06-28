package org.webapp.ecommerce.dto.errorresponse;

import java.util.ArrayList;
import java.util.List;

public class NestedErrorResponse {

    private final List<ServiceErrorResponse> listOfErrors = new ArrayList<>();

    public List<ServiceErrorResponse> getListOfErrors() {
        return listOfErrors;
    }
}

package org.webapp.ecommerce.dto.response;

import java.util.ArrayList;
import java.util.List;

public class UserDetailsResponse {

    private final List<String> ListOfUsernames = new ArrayList<>();

    public List<String> getListOfUsernames() {
        return ListOfUsernames;
    }
}

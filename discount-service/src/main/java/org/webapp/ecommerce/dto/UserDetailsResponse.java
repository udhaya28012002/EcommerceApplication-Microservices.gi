package org.webapp.ecommerce.dto;

import java.util.Map;

public class UserDetailsResponse {

    private Map<String, String> ListOfUsernames;

    public Map<String, String> getListOfUsernames() {
        return ListOfUsernames;
    }

    public void setListOfUsernames(Map<String, String> listOfUsernames) {
        ListOfUsernames = listOfUsernames;
    }
}

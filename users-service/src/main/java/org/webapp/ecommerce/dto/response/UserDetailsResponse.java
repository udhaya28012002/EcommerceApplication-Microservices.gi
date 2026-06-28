package org.webapp.ecommerce.dto.response;

import java.util.List;

public class UserDetailsResponse {

    private List<String> ListOfUsernames;

    public List<String> getListOfUsernames() {
        return ListOfUsernames;
    }

    public void setListOfUsernames(List<String> listOfUsernames) {
        ListOfUsernames = listOfUsernames;
    }
}

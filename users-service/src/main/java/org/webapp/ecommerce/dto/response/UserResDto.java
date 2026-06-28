package org.webapp.ecommerce.dto.response;


import org.springframework.cglib.core.Local;
import org.webapp.ecommerce.entity.Address;

import java.time.LocalDateTime;
import java.util.List;


public class UserResDto {

    private String name;

    private String userName;

    private String emailId;

    private String contactNo;

    private List<Address> address;

    private LocalDateTime createdAt;

    public UserResDto(String name, String userName, String emailId, String contactNo, List<Address> address, LocalDateTime createdAt) {
        this.name = name;
        this.userName = userName;
        this.emailId = emailId;
        this.contactNo = contactNo;
        this.address = address;
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getContactNo() {
        return contactNo;
    }

    public List<Address> getAddress() {
        return address;
    }
}

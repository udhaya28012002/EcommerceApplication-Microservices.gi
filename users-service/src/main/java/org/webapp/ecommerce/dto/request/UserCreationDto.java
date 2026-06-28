package org.webapp.ecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.webapp.ecommerce.entity.Address;

public class UserCreationDto {

    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Name length should be within 50 characters")
    private String name;

    @NotBlank(message = "Username is required")
    @Size(min=5, max = 15, message = "Username should be 5-15 characters only")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can contain only letters, numbers, underscore")
    private String userName;


    @Email(message = "Should be a valid Email ID")
    private String emailId;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be 8+ chars with uppercase, lowercase, number, and special character"
    )
    private String password;

    @NotBlank(message = "Confirm Password is required")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
            message = "Password must be 8+ chars with uppercase, lowercase, number, and special character"
    )
    private String confirmPassword;

    @Pattern(
            regexp = "^(\\+91|91)?[6-9]\\d{9}$",
            message = "Invalid mobile number"
    )
    private String contactNo;

    @Valid
    private Address address;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password.trim();
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name.trim();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName.trim();
    }

    public String getEmailId() {
        return emailId;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId.trim().toLowerCase();
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo.trim();
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }
}

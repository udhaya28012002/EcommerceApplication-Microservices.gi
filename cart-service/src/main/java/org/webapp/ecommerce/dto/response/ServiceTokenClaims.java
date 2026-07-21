package org.webapp.ecommerce.dto.response;

import java.util.List;

public class ServiceTokenClaims {

    private String       svc;
    private List<String> allowedServices;
    private List<String> roles;
    private String       username;
    private String       role;

    public ServiceTokenClaims(String svc, String username, String role) {
        this.svc = svc;
        this.username = username;
        this.role = role;
        this.roles = List.of(role);
    }

    public ServiceTokenClaims(String svc, List<String> allowedServices, String username, String role) {
        this.svc = svc;
        this.allowedServices = allowedServices;
        this.username = username;
        this.role = role;
        this.roles = List.of(role);
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public String getSvc() {
        return svc;
    }

    public void setSvc(String svc) {
        this.svc = svc;
    }

    public List<String> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(List<String> allowedServices) {
        this.allowedServices = allowedServices;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

package org.webapp.ecommerce.util.loadconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@ConfigurationProperties(prefix = "known-services")
public class InitAllowedServicesProperties {

    private Set<String> allowedServices;

    public Set<String> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(Set<String> allowedServices) {
        this.allowedServices = allowedServices;
    }
}

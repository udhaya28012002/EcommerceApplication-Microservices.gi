package org.webapp.ecommerce.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.webapp.ecommerce.dto.response.ServiceTokenClaims;
import org.webapp.ecommerce.util.apiConfig.APITokenFilter;
import org.webapp.ecommerce.util.apiConfig.UserAPITokenProvider;
import org.webapp.ecommerce.util.internalConfig.ServiceTokenFilter;
import org.webapp.ecommerce.util.internalConfig.UsersServiceTokenProvider;

import javax.swing.plaf.PanelUI;

@Configuration
public class UtilBeans {

    @Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper();
    }

    @Bean
    public APITokenFilter apiTokenFilter(UserAPITokenProvider userAPITokenProvider){
        return new APITokenFilter(userAPITokenProvider);
    }

    @Bean
    public FilterRegistrationBean<APITokenFilter> apiTokenFilterRegistration(APITokenFilter filter) {
        FilterRegistrationBean<APITokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Spring Security manages this, not servlet container
        return registration;
    }

    @Bean
    public ServiceTokenFilter serviceTokenFilter(UsersServiceTokenProvider usersServiceTokenProvider){
        return new ServiceTokenFilter(usersServiceTokenProvider);
    }

    @Bean
    public FilterRegistrationBean<ServiceTokenFilter> serviceTokenFilterRegistration(ServiceTokenFilter filter) {
        FilterRegistrationBean<ServiceTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false); // Spring Security manages this, not servlet container
        return registration;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();
        return new RestTemplate(factory);
    }
}

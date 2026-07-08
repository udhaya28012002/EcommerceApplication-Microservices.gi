package org.webapp.ecommerce.service;

import org.webapp.ecommerce.client.UserServiceClient;
import org.webapp.ecommerce.dto.request.*;
import org.webapp.ecommerce.dto.response.UserDetailsDtoResponse;
import org.webapp.ecommerce.enums.Role;
import org.webapp.ecommerce.enums.UserStatus;
import org.webapp.ecommerce.dto.response.InfoDto;
import org.webapp.ecommerce.dto.response.UserDetailsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.dto.response.UserResDto;
import org.webapp.ecommerce.entity.Address;
import org.webapp.ecommerce.entity.Users;
import org.webapp.ecommerce.exception.*;
import org.webapp.ecommerce.repository.UserRepo;
import org.webapp.ecommerce.util.CurrentUserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
@Service
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;

    private final CurrentUserService currentUserService;

    private final UserServiceClient userServiceClient;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, CurrentUserService currentUserService, UserServiceClient userServiceClient) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.currentUserService = currentUserService;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);
        return findByUsername_ForInternal(username);
    }

    public Users createUser(UserCreationDto dto, boolean isAdmin, long cartId) {

        log.debug("Creating new user with username: {}", dto.getUserName());

        Role role = isAdmin ? Role.ROLE_ADMIN : Role.ROLE_CUSTOMER;

        if (isPasswordMismatch(dto.getPassword(), dto.getConfirmPassword())) {
            log.warn("Password mismatch for username: {}", dto.getUserName());
            throw new PasswordMismatchException("Password and Confirm Password must match");
        }

        if (isUsernameAvailable(dto.getUserName()) || isEmailIdAlreadyRegistered(dto.getEmailId()) || isContactNoIsAlreadyRegistered(dto.getContactNo())) {
            log.warn("User registration failed. Username or Email or ContactNo already exists: {}", dto.getUserName());
            throw new ResourceAlreadyExistsException("Username (or) Email (or) Contact No is already registered");
        }

        ArrayList<Address> addresses = new ArrayList<>();

        if (dto.getAddress() == null) {
            log.warn("Address not provided for username: {}", dto.getUserName());
            throw new AddressNotFoundException("Address cannot be null");
        }

        Address address = dto.getAddress();
        address.setDefault(true);
        addresses.add(address);

        Users user = new Users(
                dto.getName(),
                dto.getUserName(),
                dto.getEmailId(),
                dto.getContactNo(),
                addresses,
                passwordEncoder.encode(dto.getPassword()),
                role,
                UserStatus.ACTIVE.name(),
                LocalDateTime.now(),
                cartId
        );

        Users savedUser = userRepo.save(user);

        log.info("User created successfully with username: {}", savedUser.getUserName());

        return savedUser;
    }

    public Page<UserResDto> getAllUsers(int page, int size) {

        log.debug("Fetching all users - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);

        Page<Users> allUsers = userRepo.findAll(pageable);

        if (allUsers.isEmpty()) {
            log.warn("No users found in database");
            throw new ResourceNotFoundException("There are no users in the database");
        }

        return allUsers.map(user -> new UserResDto(
                user.getName(),
                user.getUserName(),
                user.getEmailId(),
                user.getContactNo(),
                user.getAddress(),
                user.getCreatedAt()
        ));
    }

    public List<Users> findAllUsers_ForInternal(){
        log.debug("Fetching all users for internal use");
        return userRepo.findAll();
    }

    public Users findByUsername_ForInternal(String username){

        log.debug("Fetching user internally with username: {}", username);

        Users user = userRepo.findByUserName(username);

        if (user == null) {
            log.warn("Username not found: {}", username);
            throw new UsernameNotFoundException("Username not found!");
        }

        if(user.getStatus().equals(UserStatus.DEACTIVATED.name())){
            log.warn("Inactive account access attempt for username: {}", username);
            throw new DisabledException("Account inactive");
        }

        return user;
    }

    public UserResDto getUserDetails() {

        String loggedUser = currentUserService.getLoggedInUser();

        log.info("Fetching user details for username: {}", loggedUser);

        Users user = userRepo.findByUserName(loggedUser);

        if (user == null) {
            log.warn("User details not found for username: {}", loggedUser);
            throw new ResourceNotFoundException("User not found");
        }

        return populateUserDto(user);
    }

    @Transactional
    public InfoDto changePassword(ChangePasswordRequest changePasswordRequest) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.info("Password change requested for username: {}", loggedUser);

        if (isPasswordMismatch(changePasswordRequest.getNewPassword(), changePasswordRequest.getConfirmPassword())) {
            log.warn("Password mismatch while changing password for username: {}", loggedUser);
            throw new PasswordMismatchException("Password and Confirm Password do not match");
        }

        Users user = userRepo.findByUserName(loggedUser);

        if (user == null || !validatePassword(user, changePasswordRequest.getOldPassword())) {
            log.warn("Invalid old password provided for username: {}", loggedUser);
            throw new InvalidCredentialsException("Invalid username or password");
        }

        if(passwordEncoder.matches(
                changePasswordRequest.getNewPassword(),
                user.getPassword())) {

            log.warn("Password reuse attempt for username: {}", loggedUser);

            throw new PasswordReuseException(
                    "New password cannot be same as old password"
            );
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));

        log.info("Password changed successfully for username: {}", loggedUser);

        return new InfoDto("Password changed successfully");
    }

    @Transactional
    public InfoDto updateContactNo(ChangeOtherDetailsReq changeOtherDetailsReq) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.info("Updating contact number for username: {}", loggedUser);

        Users user = userRepo.findByUserName(loggedUser);

        if (user == null) {
            log.warn("User not found while updating contact number for username: {}", loggedUser);
            throw new ResourceNotFoundException("User not found");
        }

        user.setContactNo(changeOtherDetailsReq.getChangeField());

        log.info("Contact number updated successfully for username: {}", loggedUser);

        return new InfoDto("ContactNo changed successfully");
    }

    @Transactional
    public InfoDto addAddress(AddAddressReq addAddressReq) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.info("Adding new address for username: {}", loggedUser);

        Users user = userRepo.findByUserName(loggedUser);

        if (user == null) {
            log.warn("User not found while adding address for username: {}", loggedUser);
            throw new ResourceNotFoundException("User not found");
        }

        user.getAddress().forEach(addr -> addr.setDefault(false));

        Address newAddress = new Address();
        newAddress.setCity(addAddressReq.getCity());
        newAddress.setPincode(addAddressReq.getPincode());
        newAddress.setState(addAddressReq.getState());
        newAddress.setStreet(addAddressReq.getStreet());
        newAddress.setDefault(true);

        if (user.getAddress() == null) {
            user.setAddress(new ArrayList<>());
        }

        user.getAddress().add(newAddress);

        log.info("New address added successfully for username: {}", loggedUser);

        return new InfoDto("New Address has been added successfully");
    }

    /*public UserRegistrationTimeResponse getCreationTime(String username){
        Users user = userRepo.findByUserName(username);
        UserRegistrationTimeResponse userRegistrationTimeResponse = new UserRegistrationTimeResponse();
        userRegistrationTimeResponse.setUsername(username);
        userRegistrationTimeResponse.setCreatedAt(user.getCreatedAt());
        return userRegistrationTimeResponse;
    }*/

    public UserDetailsDtoResponse getUserDetails(String username){
        Users user  = userRepo.findByUserName(username);

        return new UserDetailsDtoResponse(
                user.getName(),
                username,
                user.getEmailId(),
                user.getContactNo()
        );
    }

    public UserDetailsResponse getAllUsernames(int page, int size){

        Page<Object[]> pages = userRepo.findAllUsernamesAndEmails(PageRequest.of(page, size));

        HashMap<String , String > usersMap = new HashMap<>();

        for (Object[] row : pages) {
            usersMap.put((String) row[0], (String) row[1]);
        }

        UserDetailsResponse allUserDetailsResponse = new UserDetailsResponse();
        allUserDetailsResponse.setListOfUsernames(usersMap);

        return allUserDetailsResponse;
    }

    @Transactional
    public InfoDto deleteUser() {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.info("Account deactivation requested for username: {}", loggedUser);

        Users user = userRepo.findByUserName(loggedUser);

        if (user == null) {
            log.warn("User not found while deleting account for username: {}", loggedUser);
            throw new InvalidCredentialsException("UserNotFound");
        }

        //We have to deactivate the Discounts and the Cart

        userServiceClient.deactivateCart(loggedUser, role);

        user.setStatus(UserStatus.DEACTIVATED.name());

        SecurityContextHolder.clearContext();

        log.info("Account deactivated successfully for username: {}", loggedUser);

        return new InfoDto("Account deactivated successfully");
    }

    private boolean isUsernameAvailable(String username) {
        return userRepo.existsByUserName(username);
    }

    private boolean isEmailIdAlreadyRegistered(String emailId) {
        return userRepo.existsByEmailId(emailId);
    }

    private boolean isContactNoIsAlreadyRegistered(String contactNo) {
        return userRepo.existsByContactNo(contactNo);
    }

    private boolean isPasswordMismatch(String password, String confirmPassword) {
        return !password.equalsIgnoreCase(confirmPassword);
    }

    private boolean validatePassword(Users user, String givenPassword) {
        return passwordEncoder.matches(givenPassword, user.getPassword());
    }

    private boolean isAdmin(LoginReqDto loginReq) {
        Users user = userRepo.findByUserName(loginReq.getUsername());

        if (!validatePassword(user, loginReq.getPassword())) {
            return false;
        }

        return user.getRole() == Role.ROLE_ADMIN;
    }

    private UserResDto populateUserDto(Users user) {
        return new UserResDto(
                user.getName(),
                user.getUserName(),
                user.getEmailId(),
                user.getContactNo(),
                user.getAddress(),
                user.getCreatedAt()
        );
    }
}
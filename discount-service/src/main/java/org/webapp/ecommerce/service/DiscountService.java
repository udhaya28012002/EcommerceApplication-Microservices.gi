package org.webapp.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.client.DiscountServiceClient;
import org.webapp.ecommerce.dto.*;
import org.webapp.ecommerce.dto.kafkadto.CouponAssignedEvent;
import org.webapp.ecommerce.entity.DiscountOnUsers;
import org.webapp.ecommerce.entity.DiscountType;
import org.webapp.ecommerce.exception.DiscountNotApplicable;
import org.webapp.ecommerce.exception.DuplicateDiscountException;
import org.webapp.ecommerce.exception.NoCouponAvailable;
import org.webapp.ecommerce.kafka.KafkaService;
import org.webapp.ecommerce.repository.GlobalDiscountRepo;
import org.webapp.ecommerce.repository.UserDiscountRepo;
import org.webapp.ecommerce.util.CurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscountService {

    private static final Logger log = LoggerFactory.getLogger(DiscountService.class);

    private final GlobalDiscountRepo globalDiscountRepo;
    private final UserDiscountRepo userDiscountRepo;
    private final CurrentUserService currentUserService;
    private final DiscountServiceClient discountServiceClient;
    private final KafkaService kafkaService;

    private final String NEW_USER_COUPON = "WelcomeGift";
    private final long NEW_USER_COUPON_VALUE = 100;

    public DiscountService(GlobalDiscountRepo globalDiscountRepo, UserDiscountRepo userDiscountRepo, CurrentUserService currentUserService, DiscountServiceClient discountServiceClient, KafkaService kafkaService) {
        this.globalDiscountRepo = globalDiscountRepo;
        this.userDiscountRepo = userDiscountRepo;
        this.currentUserService = currentUserService;
        this.discountServiceClient = discountServiceClient;
        this.kafkaService = kafkaService;
    }

    @Transactional
    public void assignWelcomeCoupon(LocalDateTime registrationTime) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Assigning welcome coupon for user: {}", loggedUser);

        //Need to get the User Details for Assigning the Coupons:

        LocalDateTime now = LocalDateTime.now();

        if (registrationTime.isBefore(now.minusDays(1))) {

            log.warn("Welcome coupon not applicable. User '{}' registered before the eligibility period.", loggedUser);

            throw new DiscountNotApplicable("Welcome coupon is available only for newly registered users.");
        }

        if (userDiscountRepo.existsByUsernameAndCouponCode(loggedUser, NEW_USER_COUPON)) {

            log.warn("Welcome coupon already assigned for user: {}", loggedUser);

            throw new DuplicateDiscountException("Discount already exists for this User");
        }

        userDiscountRepo.save(createDiscount(loggedUser, NEW_USER_COUPON, "Welcome Gift", DiscountType.FLAT, NEW_USER_COUPON_VALUE, 200, 100, 1, LocalDate.from(now.plusMonths(6))));

        log.info("Welcome coupon assigned successfully for user: {}", loggedUser);

    }

    public void assignDiscountToCustomers() {
        //Need to enhance this with the RABBITMQ to make the task to happen later and ack the user immediately for better users experience.
    }

    @Transactional
    public boolean assignDiscountToAllUsers(AddDiscountDto dto) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        String couponCode = dto.getCouponCode();

        log.debug("Assigning coupon to all users. CouponCode: {}", couponCode);

        Map<String, String> allUsernames = discountServiceClient.getAllUsernames(loggedUser, role).getListOfUsernames();

        Set<String> existingUserIds = new HashSet<>(userDiscountRepo.findUsernamesByCouponCode(couponCode));

        List<DiscountOnUsers> discountedUsers = new ArrayList<>();

        LocalDate expiry = LocalDate.now().plusMonths(dto.getValidityInMonths());

        Map<String, String> couponAssignedUsers = new HashMap<>();

        for (Map.Entry<String, String> user : allUsernames.entrySet()) {

            String username = user.getKey();
            String email = user.getValue();

            if (existingUserIds.contains(username)) {

                log.warn("Coupon already exists for user: {}", username);

                continue;
            }

            couponAssignedUsers.put(username, email);

            discountedUsers.add(createDiscount(
                    username,
                    couponCode,
                    dto.getDescription(),
                    dto.getDiscountType(),
                    dto.getDiscountValue(),
                    dto.getMinAmtOrder(),
                    dto.getMaxDiscountAmount(),
                    dto.getUsageLimit(),
                    expiry
            ));

        }

        if (discountedUsers.isEmpty()) {
            log.info("Coupon '{}' was not assigned because every eligible user already has it.", couponCode);
            return false;
        }

        CouponAssignedEvent couponAssignedDetails = new CouponAssignedEvent(
                couponCode,
                dto.getDescription(),
                dto.getDiscountType().name(),
                dto.getDiscountValue(),
                dto.getMinAmtOrder(),
                dto.getMaxDiscountAmount(),
                expiry,
                dto.getUsageLimit(),
                couponAssignedUsers
        );

        // Save to database first
        userDiscountRepo.saveAll(discountedUsers);

        //KAFKA INTEGRATION SERVICE
        kafkaService.sendMessage(
                couponAssignedDetails,
                "coupon.assigned",
                couponCode
        );

        log.info(
                "Coupon '{}' assigned successfully to {} users.",
                couponCode,
                discountedUsers.size()
        );

        return true;

    }

    @Transactional
    public void assignDiscountToEligibleUsers(AddDiscountDto dto, double filterPrice) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        String couponCode = dto.getCouponCode();

        log.debug("Assigning coupon to eligible users. CouponCode: {}, FilterPrice: {}", couponCode, filterPrice);

        Map<String, String> filteredUsers = discountServiceClient.filterUsernameByOrderAmt(filterPrice, loggedUser, role).getListOfUsernames();

        Set<String> existingUserIds = new HashSet<>(userDiscountRepo.findUsernamesByCouponCode(couponCode));

        List<DiscountOnUsers> discountedUsers = new ArrayList<>();

        LocalDate expiry = LocalDate.now().plusMonths(dto.getValidityInMonths());

        Map<String, String> couponAssignedUsers = new HashMap<>();

        for (Map.Entry<String, String> user : filteredUsers.entrySet()) {

            String username = user.getKey();

            if (existingUserIds.contains(username)) {

                log.warn("Coupon '{}' already assigned for user: {}", couponCode, username);
                continue;
            }

            couponAssignedUsers.put(username, filteredUsers.get(username));

            DiscountOnUsers discountOnUsers = createDiscount(
                    username,
                    couponCode,
                    dto.getDescription(),
                    dto.getDiscountType(),
                    dto.getDiscountValue(),
                    dto.getMinAmtOrder(),
                    dto.getMaxDiscountAmount(),
                    dto.getUsageLimit(),
                    expiry
            );

            discountedUsers.add(discountOnUsers);

        }

        if (discountedUsers.isEmpty()) {
            log.info("No eligible users found for coupon assignment.");
            return;
        }

        // Save to database first
        userDiscountRepo.saveAll(discountedUsers);

        CouponAssignedEvent couponAssignedDetails = new CouponAssignedEvent(
                couponCode,
                dto.getDescription(),
                dto.getDiscountType().name(),
                dto.getDiscountValue(),
                dto.getMinAmtOrder(),
                dto.getMaxDiscountAmount(),
                expiry,
                dto.getUsageLimit(),
                couponAssignedUsers
        );

        //KAFKA INTEGRATION SERVICE
        kafkaService.sendMessage(couponAssignedDetails, "coupon.assigned", couponCode);

        log.info(
                "Coupon '{}' assigned successfully to {} eligible users.",
                couponCode,
                discountedUsers.size()
        );
    }

    private DiscountOnUsers createDiscount(
            String username,
            String code,
            String description,
            DiscountType discountType,
            long discountValue,
            double minOrderAmount,
            double maxDiscountAmount,
            int usageLimit,
            LocalDate endDate
    ) {
        return new DiscountOnUsers(
                code,
                description,
                discountType,
                BigDecimal.valueOf(discountValue),
                BigDecimal.valueOf(minOrderAmount),
                BigDecimal.valueOf(maxDiscountAmount),
                LocalDate.now(),
                endDate,
                usageLimit,
                0,
                true,
                username
        );
    }

    @Transactional
    public boolean revertCoupon(String coupon) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug(
                "Reverting coupon '{}' for user '{}'.",
                coupon,
                loggedUser
        );

        int reactivatedRows = userDiscountRepo.reactivateCoupon(loggedUser, coupon);

        int decrementedRows = userDiscountRepo.decrementUsageCount(loggedUser, coupon);

        boolean reverted = reactivatedRows > 0 && decrementedRows > 0;

        if (!reverted) {

            log.warn(
                    "Failed to revert coupon '{}' for user '{}'.",
                    coupon,
                    loggedUser
            );

            return false;
        }

        log.info(
                "Coupon '{}' reverted successfully for user '{}'.",
                coupon,
                loggedUser
        );

        return true;
    }

    public boolean couponExists(String coupon) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Validating coupon: {} for user: {}", coupon, loggedUser);

        boolean valid = userDiscountRepo.existsByUsernameAndCouponCode(loggedUser, coupon);

        log.info("Coupon validation result for {} : {}", coupon, valid);

        return valid;
    }

    public DisplayCouponsRes displayCoupons() {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Fetching coupons for user: {}", loggedUser);

        List<DiscountOnUsers> discountOnUsers = userDiscountRepo.findByUsername(loggedUser);

        if (discountOnUsers.isEmpty()) {

            log.warn("No coupons available for user: {}", loggedUser);

            throw new NoCouponAvailable("No applicable coupon available");
        }

        log.info("Coupons fetched successfully for user: {}", loggedUser);

        return buildCouponDetailsRes(discountOnUsers);
    }

    public DisplayCouponsRes displayAllCoupons() {

        log.debug("Fetching all coupons");

        List<DiscountOnUsers> discountOnUsers = userDiscountRepo.findAll();

        log.info("Total coupons fetched: {}", discountOnUsers.size());

        return buildCouponDetailsRes(discountOnUsers);
    }


    @Transactional
    public ApplyCouponResponse applyDiscountByUsers(String couponCode, double purchasePrice) {

        String loggedUser = currentUserService.getLoggedInUser();
        LocalDate today = LocalDate.now();

        log.debug(
                "Applying coupon '{}' for user '{}' with purchase price {}.",
                couponCode,
                loggedUser,
                purchasePrice
        );

        Optional<DiscountOnUsers> userDiscount =
                userDiscountRepo.findByUsernameAndCouponCodeAndActiveTrue(
                        loggedUser,
                        couponCode
                );

        if (userDiscount.isEmpty()) {

            log.warn(
                    "Coupon '{}' not found for user '{}'.",
                    couponCode,
                    loggedUser
            );

            return failureResponse(couponCode, purchasePrice);
        }

        DiscountOnUsers discount = userDiscount.get();

        boolean validCoupon =
                !discount.getEndDate().isBefore(today)
                        && purchasePrice >= discount.getMinimumOrderAmount().doubleValue()
                        && discount.getUsedCount() < discount.getUsageLimit();

        if (!validCoupon) {

            log.warn(
                    "Coupon '{}' is not valid for user '{}'.",
                    couponCode,
                    loggedUser
            );

            return failureResponse(couponCode, purchasePrice);
        }

        double calculatedDiscount =
                discount.getDiscountType() == DiscountType.FLAT
                        ? discount.getDiscountValue().doubleValue()
                        : purchasePrice * discount.getDiscountValue().doubleValue() / 100;

        double appliedDiscount = Math.min(
                calculatedDiscount,
                discount.getMaximumDiscountAmount().doubleValue()
        );

        double finalPrice = Math.max(0, purchasePrice - appliedDiscount);

        int usedCount = discount.getUsedCount() + 1;
        discount.setUsedCount(usedCount);

        if (usedCount >= discount.getUsageLimit()) {

            discount.setActive(false);

            log.info(
                    "Coupon '{}' has reached its usage limit and is now inactive.",
                    couponCode
            );
        }

        ApplyCouponResponse response = new ApplyCouponResponse();
        response.setApplied(true);
        response.setCouponName(couponCode);
        response.setMessage(couponCode + " code applied successfully.");
        response.setFinalPrice(finalPrice);

        log.info(
                "Coupon '{}' applied successfully for user '{}'. Final price: {}",
                couponCode,
                loggedUser,
                finalPrice
        );

        return response;
    }

    private ApplyCouponResponse failureResponse(String couponCode, double purchasePrice) {

        log.warn("Coupon application failed. CouponCode: {}", couponCode);

        ApplyCouponResponse response = new ApplyCouponResponse();

        response.setCouponName("No Coupon is used");
        response.setApplied(false);
        response.setMessage("Coupon Code Is Not Available");
        response.setFinalPrice(purchasePrice);

        return response;
    }

    private DisplayCouponsRes buildCouponDetailsRes(List<DiscountOnUsers> discounts) {
        DisplayCouponsRes displayCouponsRes = new DisplayCouponsRes();

        Map<String, CouponDetailsRes> couponDetailsMap = discounts.stream()
                .collect(Collectors.toMap(
                        discount -> discount.getCouponCode(),
                        discount ->
                                new CouponDetailsRes(
                                        discount.getDescription(),
                                        discount.getDiscountType(),
                                        discount.getDiscountValue(),
                                        discount.getMinimumOrderAmount(),
                                        discount.getMaximumDiscountAmount(),
                                        discount.getEndDate(),
                                        discount.getActive(),
                                        discount.getUsageLimit()
                                ),
                        (existing, duplicate) -> existing
                ));

        displayCouponsRes.setAvailableCoupons(couponDetailsMap);

        return displayCouponsRes;
    }

    @Transactional
    public ApplyCouponResponse checkCouponsAndRedeem(String coupon, double totalPricePerOrder) {

        log.debug("Checking coupon '{}'.", coupon);

        ApplyCouponResponse applyCouponResponse = applyDiscountByUsers(coupon, totalPricePerOrder);

        if (!applyCouponResponse.isApplied()) {

            log.warn(
                    "Coupon '{}' could not be applied. Reason: {}",
                    coupon,
                    applyCouponResponse.getMessage()
            );
        }

        return applyCouponResponse;
    }
}

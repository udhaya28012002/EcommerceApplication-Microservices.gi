package org.webapp.ecommerce.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.webapp.ecommerce.client.DiscountServiceClient;
import org.webapp.ecommerce.dto.AddDiscountDto;
import org.webapp.ecommerce.dto.ApplyCouponResponse;
import org.webapp.ecommerce.dto.CouponDetailsRes;
import org.webapp.ecommerce.dto.DisplayCouponsRes;
import org.webapp.ecommerce.entity.DiscountOnUsers;
import org.webapp.ecommerce.entity.DiscountType;
import org.webapp.ecommerce.exception.DiscountNotApplicable;
import org.webapp.ecommerce.exception.DuplicateDiscountException;
import org.webapp.ecommerce.exception.NoCouponAvailable;
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

    private final String NEW_USER_COUPON = "WelcomeGift";
    private final long NEW_USER_COUPON_VALUE = 100;

    public DiscountService(GlobalDiscountRepo globalDiscountRepo, UserDiscountRepo userDiscountRepo, CurrentUserService currentUserService, DiscountServiceClient discountServiceClient) {
        this.globalDiscountRepo = globalDiscountRepo;
        this.userDiscountRepo = userDiscountRepo;
        this.currentUserService = currentUserService;
        this.discountServiceClient = discountServiceClient;
    }

    public void assignWelcomeCoupon(LocalDateTime registrationTime) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Assigning welcome coupon for user: {}", loggedUser);

        //Need to get the User Details for Assigning the Coupons:

        if (registrationTime.isBefore(LocalDateTime.now().minusDays(1))) {

            log.warn("Welcome coupon not applicable for user: {}", loggedUser);

            throw new DiscountNotApplicable("Welcome Gift is only new users.");
        }

        if (userDiscountRepo.existsByUsernameAndCouponCode(loggedUser, NEW_USER_COUPON)) {

            log.warn("Welcome coupon already assigned for user: {}", loggedUser);

            throw new DuplicateDiscountException("Discount already exists for this User");
        }

        DiscountOnUsers discountOnUsers = createDiscount(loggedUser, NEW_USER_COUPON, "Welcome Gift", DiscountType.FLAT, NEW_USER_COUPON_VALUE, 200, 100, 1, LocalDate.now().plusMonths(6));

        userDiscountRepo.save(discountOnUsers);

        log.info("Welcome coupon assigned successfully for user: {}", loggedUser);

    }

    @Transactional
    public boolean assignDiscountToAllUsers(AddDiscountDto dto) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Assigning coupon to all users. CouponCode: {}", dto.getCouponCode());

        List<String> allUsernames = discountServiceClient.getAllUsernames(loggedUser, role).getListOfUsernames();

        List<String> existingUserIds = userDiscountRepo.findUsernamesByCouponCode(dto.getCouponCode());

        List<DiscountOnUsers> discountedUsers = new ArrayList<>();

        LocalDate expiry = LocalDate.now().plusMonths(dto.getValidityInMonths());

        for (String username : allUsernames) {

            if (existingUserIds.contains(username)) {

                log.warn("Coupon already exists for user: {}", username);

                continue;
            }

            discountedUsers.add(createDiscount(
                    username,
                    dto.getCouponCode(),
                    dto.getDescription(),
                    dto.getDiscountType(),
                    dto.getDiscountValue(),
                    dto.getMinAmtOrder(),
                    dto.getMaxDiscountAmount(),
                    dto.getUsageLimit(),
                    expiry
            ));

        }

        userDiscountRepo.saveAll(discountedUsers);

        if(!discountedUsers.isEmpty()) {
            log.info("Coupons assigned successfully to {} users", discountedUsers.size());
            return true;
        }
        else {
            log.info("No users are available/Coupon Already added : {} users", 0);
            return false;
        }

    }

    @Transactional
    public void assignDiscountToEligibleUsers(AddDiscountDto addDiscountDto, double filterPrice) {

        String loggedUser = currentUserService.getLoggedInUser();
        String role = currentUserService.getLoggedInUserRole();

        log.debug("Assigning coupon to eligible users. CouponCode: {}, FilterPrice: {}", addDiscountDto.getCouponCode(), filterPrice);

        List<String> filteredUsers = discountServiceClient.filterUsernameByOrderAmt(filterPrice, loggedUser, role).getListOfUsernames();

        List<String> existingUserIds = userDiscountRepo.findUsernamesByCouponCode(addDiscountDto.getCouponCode());

        List<DiscountOnUsers> discountedUsers = new ArrayList<>();

        LocalDate expiry = LocalDate.now().plusMonths(addDiscountDto.getValidityInMonths());

        for (String username : filteredUsers) {

            if (existingUserIds.contains(username)) {

                log.warn("Coupon already assigned for user: {}", username);
                continue;
            }

            DiscountOnUsers discountOnUsers = createDiscount(
                    loggedUser,
                    addDiscountDto.getCouponCode(),
                    addDiscountDto.getDescription(),
                    addDiscountDto.getDiscountType(),
                    addDiscountDto.getDiscountValue(),
                    addDiscountDto.getMinAmtOrder(),
                    addDiscountDto.getMaxDiscountAmount(),
                    addDiscountDto.getUsageLimit(),
                    expiry
            );

            discountedUsers.add(discountOnUsers);

        }

        userDiscountRepo.saveAll(discountedUsers);

        log.info("Coupons assigned successfully to eligible users");
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
                LocalDate.now().plusMonths(6),
                usageLimit,
                0,
                true,
                username
        );
    }

    @Transactional
    public boolean revertCoupon(String coupon) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Reverting coupon: {} for user: {}", coupon, loggedUser);

        int reactivateCoupon = userDiscountRepo.reactivateCoupon(loggedUser, coupon);
        int decrementUsageCount = userDiscountRepo.decrementUsageCount(loggedUser, coupon);

        if (reactivateCoupon == 0 || decrementUsageCount == 0) {
            log.info("Coupon Reverted Failed for {} : {}", loggedUser, coupon);
            return false;
        } else {
            log.info("Coupon Reverted for {} : {}", loggedUser, coupon);
            return true;
        }
    }

    public boolean validateCoupon(String coupon) {

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

        List<DiscountOnUsers> discountOnUsers =userDiscountRepo.findAll();

        log.info("Total coupons fetched: {}", discountOnUsers.size());

        return buildCouponDetailsRes(discountOnUsers);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public ApplyCouponResponse applyDiscountByUsers(String couponCode, double purchasePrice) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.debug("Applying coupon. User: {}, CouponCode: {}, PurchasePrice: {}", loggedUser, couponCode, purchasePrice);

        List<DiscountOnUsers> discountOnUser = userDiscountRepo.findByUsername(loggedUser);

        Optional<DiscountOnUsers> userDiscount = discountOnUser.stream()
                .filter((discountOnUsers -> discountOnUsers.getCouponCode().equals(couponCode) && discountOnUsers.getActive()))
                .findFirst();

        ApplyCouponResponse applyCouponResponse = new ApplyCouponResponse();

        if (userDiscount.isEmpty()) {

            log.warn("Coupon not available for user: {}", loggedUser);

            return failureResponse(couponCode, purchasePrice);
        }

        DiscountOnUsers discount = userDiscount.get();

        boolean validCoupon = (discount.getActive()
                && (!discount.getEndDate().isBefore(LocalDate.now()))
                && (purchasePrice >= discount.getMinimumOrderAmount().doubleValue())
                && (discount.getUsedCount() < discount.getUsageLimit())
        );

        if (!validCoupon) {

            log.warn("Coupon validation failed for user: {}", loggedUser);

            return failureResponse(couponCode, purchasePrice);
        }

        DiscountType discountType = discount.getDiscountType();

        double discountValue = discountType.equals(DiscountType.FLAT) ? discount.getDiscountValue().doubleValue() : (purchasePrice * discount.getDiscountValue().doubleValue()) / 100;

        double finalPriceAfterDiscounts = (discount.getMaximumDiscountAmount().doubleValue() > discountValue) ? purchasePrice - discountValue : purchasePrice - discount.getMaximumDiscountAmount().doubleValue();

        discount.setUsedCount(discount.getUsedCount() + 1);

        if (discount.getUsedCount() >= discount.getUsageLimit()) {
            discount.setActive(false);

            log.info("Coupon deactivated after reaching usage limit. CouponCode: {}", couponCode);
        }

        applyCouponResponse.setApplied(true);
        applyCouponResponse.setCouponName(couponCode);
        applyCouponResponse.setMessage(discount.getCouponCode() + " code is applied");
        applyCouponResponse.setFinalPrice(finalPriceAfterDiscounts);

        log.info("Coupon applied successfully. User: {}, CouponCode: {}, FinalPrice: {}", loggedUser, couponCode, finalPriceAfterDiscounts);

        return applyCouponResponse;
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
        log.debug("Validating coupon: {}", coupon);


        if (!validateCoupon(coupon)) {

            log.info("Coupon Code Is Not Available");

            ApplyCouponResponse couponResponse = new ApplyCouponResponse();
            couponResponse.setApplied(false);
            couponResponse.setMessage("No Coupon is applied");
            couponResponse.setFinalPrice(totalPricePerOrder);
            return couponResponse;
        }

        ApplyCouponResponse applyCouponResponse = applyDiscountByUsers(coupon, totalPricePerOrder);

        if (!applyCouponResponse.isApplied()) {

            log.warn("Coupon application failed: {}", applyCouponResponse.getMessage());

            log.info(applyCouponResponse.getMessage());
        }

        return applyCouponResponse;
    }
}

package org.webapp.ecommerce.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.webapp.ecommerce.dto.AddDiscountDto;
import org.webapp.ecommerce.service.DiscountService;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/coupon")
public class DiscountController {

    private static final Logger log = LoggerFactory.getLogger(DiscountController.class);

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @PostMapping("/addCoupons/{filterPrice}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignDiscountToUsers(@Valid @RequestBody AddDiscountDto addDiscountDto, @PathVariable double filterPrice){

        log.debug("Assign coupon to eligible users request received. FilterPrice: {}, CouponCode: {}", filterPrice, addDiscountDto.getCouponCode());

        discountService.assignDiscountToEligibleUsers(addDiscountDto, filterPrice);

        log.info("Coupons assigned successfully to eligible users. CouponCode: {}", addDiscountDto.getCouponCode());

        return ResponseEntity.status(HttpStatus.CREATED).body("Coupons assigned successfully");
    }

    @PostMapping("/internal/assignWelcomeCoupon")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> assignWelcomeCoupon(@RequestParam LocalDateTime registrationTime){

        log.debug("Assign coupon to new users request received.");

        discountService.assignWelcomeCoupon(registrationTime);

        log.info("Coupons assigned successfully to new users");

        return ResponseEntity.status(HttpStatus.CREATED).body("Coupons assigned successfully");
    }

    @PostMapping("/addCouponsToAllUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> assignDiscountToAllUsers(@Valid @RequestBody AddDiscountDto addDiscountDto){

        log.debug("Assign coupon to all users request received. CouponCode: {}", addDiscountDto.getCouponCode());

        boolean isSuccess = discountService.assignDiscountToAllUsers(addDiscountDto);

        if(isSuccess) {
            log.info("Coupons assigned successfully to all users. CouponCode: {}", addDiscountDto.getCouponCode());

            return ResponseEntity.status(HttpStatus.CREATED).body("Coupons assigned successfully");
        }
        else {
            log.info("Coupon assignment skipped. No users found. CouponCode: {}", addDiscountDto.getCouponCode());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No users found for adding this Coupon. Coupon was not applied.");
        }
    }

    @GetMapping("/displayCoupons")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> viewCoupons(){

        log.debug("Display coupons request received for customer");

        return ResponseEntity.ok(discountService.displayCoupons());
    }

    @GetMapping("/displayAllCoupons")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> viewAllCoupons(){

        log.debug("Display all coupons request received");

        return ResponseEntity.ok(discountService.displayAllCoupons());
    }

    @PostMapping("/internal/checkCouponsAndRedeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> checkCouponsAndRedeem(@RequestParam String coupon, @RequestParam double totalPricePerOrder){

        log.debug("Validate coupon and apply Coupon request received.");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(discountService.checkCouponsAndRedeem(coupon, totalPricePerOrder));
    }

    @PatchMapping("/internal/revertCoupons/{couponCode}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> revertCoupons(@PathVariable String couponCode){

        log.debug("Validate coupon and apply Coupon request received.");

        if(discountService.revertCoupon(couponCode)) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("Coupon Reverted Successfully");
        }
        else {
            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("Coupon Revert Failed");
        }
    }

}

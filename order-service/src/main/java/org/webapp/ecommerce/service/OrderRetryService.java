package org.webapp.ecommerce.service;

import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleStateException;
import org.webapp.ecommerce.dto.request.PlaceOrderRequest;
import org.webapp.ecommerce.dto.response.OrdersResDto;
import org.webapp.ecommerce.exception.OrderItemsNotFoundException;
import org.webapp.ecommerce.exception.OrderProcessingException;
import org.webapp.ecommerce.util.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
public class OrderRetryService {

    private final OrderService orderTransactionService;
    private final CurrentUserService currentUserService;

    private final Logger log = LoggerFactory.getLogger(OrderRetryService.class);

    public OrderRetryService(OrderService orderTransactionService, CurrentUserService currentUserService) {
        this.orderTransactionService = orderTransactionService;
        this.currentUserService = currentUserService;
    }

    public void placeOrderRetry(PlaceOrderRequest request) {

        String loggedUser = currentUserService.getLoggedInUser();

        log.info("Order retry process started for user: {}", loggedUser);

        int retries = 5;

        while (retries > 0) {

            try {
                log.info("Attempting order placement for user: {}. Remaining retries: {}", loggedUser, retries);

                orderTransactionService.placeOrderReq(request);

                return;   // ← success — exit the method entirely, no fall-through

            } catch (
                    ObjectOptimisticLockingFailureException |
                    OptimisticLockException |
                    StaleStateException ex
            ) {
                retries--;

                log.warn("Optimistic locking failure for user {}. Retries left: {}", loggedUser, retries);

                if (retries == 0) {
                    log.error("Order placement failed after maximum retries for user: {}", loggedUser);
                    throw new OrderProcessingException("Too many concurrent requests. Please try again");
                }

                try {
                    log.info("Waiting before retrying order placement for user: {}", loggedUser);
                    Thread.sleep(100);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    log.error("Order processing interrupted for user: {}", loggedUser, interruptedException);
                    throw new OrderProcessingException("Order processing interrupted");
                }

            } catch (OrderItemsNotFoundException ex) {
                log.error("Order placement failed for user: {}. Reason: {}", loggedUser, ex.getMessage());
                throw ex;

            } catch (Exception ex) {
                log.error("Error : " + ex.getMessage());
                retries--;

                if (retries == 0) {
                    throw new OrderProcessingException("Order placement failed: " + ex.getMessage());
                }
            }
        }

        // Only reached if the loop exits without success AND without throwing above
        // (shouldn't normally happen given the retries==0 checks, but kept as a safety net)
        throw new OrderProcessingException("Order placement failed after all retries");
    }
}
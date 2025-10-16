package edu.trincoll.service.policy;

import edu.trincoll.model.MembershipType;
import org.springframework.stereotype.Component;

@Component
public class CheckoutPolicyFactory {
    public CheckoutPolicy getPolicyFor(MembershipType type) {
        return switch (type) {
            case REGULAR -> new RegularCheckoutPolicy();
            case PREMIUM -> new PremiumCheckoutPolicy();
            case STUDENT -> new StudentCheckoutPolicy();
            default -> throw new IllegalArgumentException("Unknown membership type: " + type);
        };
    }
}

package edu.trincoll.service.fee;

import edu.trincoll.model.MembershipType;
import org.springframework.stereotype.Component;

@Component
public class LateFeeCalculatorFactory {
    public LateFeeCalculator getCalculatorFor(MembershipType type) {
        return switch (type) {
            case REGULAR -> new RegularLateFeeCalculator();
            case PREMIUM -> new PremiumLateFeeCalculator();
            case STUDENT -> new StudentLateFeeCalculator();
        };
    }
}

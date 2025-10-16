package edu.trincoll.service.fee;

public class PremiumLateFeeCalculator implements LateFeeCalculator {
    @Override
    public double calculateLateFee(long daysLate) {
        return 0.0; // No late fees for premium members
    }
}

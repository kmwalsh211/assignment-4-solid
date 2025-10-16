package edu.trincoll.service.fee;

public class StudentLateFeeCalculator implements LateFeeCalculator {
    private static final double LATE_FEE_PER_DAY = 0.25;

    @Override
    public double calculateLateFee(long daysLate) {
        return daysLate * LATE_FEE_PER_DAY;
    }
}

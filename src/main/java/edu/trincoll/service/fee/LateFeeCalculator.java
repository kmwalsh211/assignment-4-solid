package edu.trincoll.service.fee;

public interface LateFeeCalculator {
    double calculateLateFee(long daysLate);
}

package edu.trincoll.service.policy;

import edu.trincoll.model.Member;

public class StudentCheckoutPolicy implements CheckoutPolicy {
    private static final int MAX_BOOKS = 5;
    private static final int LOAN_PERIOD_DAYS = 21;

    @Override
    public int getMaxBooks() {
        return MAX_BOOKS;
    }

    @Override
    public int getLoanPeriodDays() {
        return LOAN_PERIOD_DAYS;
    }

    @Override
    public boolean canCheckout(Member member) {
        return member.getBooksCheckedOut() < getMaxBooks();
    }
}

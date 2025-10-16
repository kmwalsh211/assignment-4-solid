package edu.trincoll.service.policy;

import edu.trincoll.model.Member;

public class RegularCheckoutPolicy implements CheckoutPolicy {
    private static final int MAX_BOOKS = 3;
    private static final int LOAN_PERIOD_DAYS = 14;

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

package edu.trincoll.service.policy;

import edu.trincoll.model.Member;

public interface CheckoutPolicy {
    int getMaxBooks();
    int getLoanPeriodDays();
    boolean canCheckout(Member member);
}

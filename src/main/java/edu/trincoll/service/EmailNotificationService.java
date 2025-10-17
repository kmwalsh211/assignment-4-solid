package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.Member;
import edu.trincoll.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class EmailNotificationService implements NotificationService {

    @Override
    public void sendCheckoutNotification(Member member, Book book, LocalDate dueDate) {
        System.out.println("Notification for " + member.getName() + ": Book checked out: " + book.getTitle() + ". Due date: " + dueDate);
    }

    @Override
    public void sendReturnNotification(Member member, Book book, double lateFee) {
        System.out.println("Notification for " + member.getName() + ": Book returned: " + book.getTitle() + ". Late fee: " + lateFee);
    }
}

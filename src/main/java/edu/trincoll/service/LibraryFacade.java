package edu.trincoll.service;

import edu.trincoll.service.report.AvailabilityReportGenerator;
import edu.trincoll.service.report.OverdueReportGenerator;
import org.springframework.stereotype.Service;

/**
 * AI Collaboration Summary:
 *
 * Team Members and Contributions:
 * - Kayla Walsh: TODOs 1, 2, 3
 * - AJ Mitchell: TODOs 4, 5
 * - Saqlain Anjum: TODOs 6, 7, 8
 *
 * AI Tools Used: ChatGPT
 *
 * How AI Helped:
 * - I was really confused with how to design some of my tests for these
 * - implementations, so I generated ideas using ChatGPT.
 * - "Provide some suggested tests for the AvailabilityReportGenerator class
 * - "and please explain what aspects of the code it covers."
 * - Helped identify how to make tests match with the new implementations
 * - Helped keep track of the changes made to the program to write the Refactoring write up.
 *
 * What We Learned:
 - Smaller, single-purpose services (SRP) made the codebase easier
 - to reason about and test `BookSearchService`, `MemberService`, and report generators each have clear boundaries.
 - Depending on abstractions (DIP) instead of concretes let us mock everything cleanly in unit tests
 - (e.g., `NotificationService`, `LateFeeCalculator`, `CheckoutPolicy`).
 - The Strategy pattern (OCP) eliminated if/else ladders and made adding new membership types or fee rules a drop-in change.
 - Interface Segregation reduced coupling: consumers only see the methods they actually need (e.g., search vs. reporting vs. notifications).
 - A Facade over the services simplified controller code and clarified orchestration responsibilities.
 - Spring Data JPA + H2 made integration tests fast and realistic; Mockito kept unit tests laser-focused.

 * Hardest Principle & Why:
 - Open–Closed Principle (OCP) was the trickiest.
 - Untangling existing conditionals without breaking behavior
 - required introducing stable interfaces and agreeing on where
 - factories live. The challenge wasn’t writing
 - strategies—it was deciding correct seams (e.g., what belongs in `CheckoutPolicy` vs. `BookService`) so future extensions don’t
 - leak back into the core services.
 */
@Service
public class LibraryFacade {

    private final AvailabilityReportGenerator availabilityReportGenerator;
    private final OverdueReportGenerator overdueReportGenerator;

    public LibraryFacade(AvailabilityReportGenerator availabilityReportGenerator,
                         OverdueReportGenerator overdueReportGenerator) {
        this.availabilityReportGenerator = availabilityReportGenerator;
        this.overdueReportGenerator = overdueReportGenerator;
    }

    public String generateAvailabilityReport() {
        return availabilityReportGenerator.generateReport();
    }

    public String generateOverdueReport() {
        return overdueReportGenerator.generateReport();
    }
}

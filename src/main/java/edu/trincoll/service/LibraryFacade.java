package edu.trincoll.service;

import edu.trincoll.service.report.AvailabilityReportGenerator;
import edu.trincoll.service.report.OverdueReportGenerator;
import org.springframework.stereotype.Service;

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

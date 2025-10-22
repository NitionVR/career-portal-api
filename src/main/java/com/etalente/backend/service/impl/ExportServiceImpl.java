package com.etalente.backend.service.impl;

import com.etalente.backend.dto.ApplicantSummaryDto;
import com.etalente.backend.exception.ServiceException;
import com.etalente.backend.service.ExportService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class ExportServiceImpl implements ExportService {

    @Override
    public void exportToCsv(List<ApplicantSummaryDto> applicants, OutputStream outputStream) {
        try (CSVPrinter csvPrinter = new CSVPrinter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVFormat.DEFAULT.withHeader(
                    "ID", "Candidate Name", "Job Title", "Experience (Years)",
                    "Location", "Status", "Application Date"
                ))) {

            for (ApplicantSummaryDto applicant : applicants) {
                csvPrinter.printRecord(
                    applicant.getId(),
                    applicant.getCandidateName(),
                    applicant.getJobTitle(),
                    applicant.getExperienceYears(),
                    applicant.getLocation(),
                    applicant.getStatus(),
                    applicant.getApplicationDate()
                );
            }

            csvPrinter.flush();
            log.info("Exported {} applicants to CSV", applicants.size());

        } catch (IOException e) {
            log.error("Error exporting to CSV", e);
            throw new ServiceException("Failed to export applicants to CSV", e);
        }
    }

    @Override
    public void exportToExcel(List<ApplicantSummaryDto> applicants, OutputStream outputStream) {
        // Use Apache POI for Excel export
        // Implementation similar to CSV but using XSSFWorkbook
        throw new UnsupportedOperationException("Excel export not yet implemented");
    }

    @Override
    public void exportToPdf(List<ApplicantSummaryDto> applicants, OutputStream outputStream) {
        // Use iText or Apache PDFBox for PDF export
        throw new UnsupportedOperationException("PDF export not yet implemented");
    }
}

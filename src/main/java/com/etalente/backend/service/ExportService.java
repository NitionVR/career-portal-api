package com.etalente.backend.service;

import com.etalente.backend.dto.ApplicantSummaryDto;
import org.springframework.data.domain.Page;

import java.io.OutputStream;
import java.util.List;

public interface ExportService {
    void exportToCsv(List<ApplicantSummaryDto> applicants, OutputStream outputStream);
    void exportToExcel(List<ApplicantSummaryDto> applicants, OutputStream outputStream);
    void exportToPdf(List<ApplicantSummaryDto> applicants, OutputStream outputStream);
}

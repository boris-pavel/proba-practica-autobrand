package ro.autobrand.proba.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ro.autobrand.proba.dto.InvoiceLineDto;
import ro.autobrand.proba.exception.InvalidPdfException;
import ro.autobrand.proba.pdf.InvoiceParser;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfInvoiceService {

    private final InvoiceParser parser;
    private final CsvExportService csvExportService;

    public byte[] processToCsv(MultipartFile file) {
        validatePdf(file);
        try {
            List<InvoiceLineDto> lines = parser.parse(file.getInputStream());
            log.info("Parsed {} lines from '{}'", lines.size(), file.getOriginalFilename());
            return csvExportService.toCsv(lines);
        } catch (IOException e) {
            throw new InvalidPdfException("Eroare la citirea PDF-ului: " + e.getMessage());
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidPdfException("Selectează un fișier.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidPdfException("Fișierul trebuie să fie PDF.");
        }
    }
}
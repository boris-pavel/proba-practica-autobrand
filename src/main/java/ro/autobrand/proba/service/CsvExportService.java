package ro.autobrand.proba.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class CsvExportService {

    private static final String[] HEADER = {
            "cod_produs", "denumire", "pret_unitar", "moneda", "cantitate"
    };

    public byte[] toCsv(List<InvoiceLineDto> lines) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // UTF-8 BOM (Excel îl interpretează ca semnal de encoding corect pentru diacritice)
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader(HEADER).build())) {
            for (InvoiceLineDto line : lines) {
                printer.printRecord(
                        line.getProductCode(),
                        line.getProductName(),
                        line.getUnitPrice(),
                        line.getCurrency(),
                        line.getQuantity()
                );
            }
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException("CSV generation failed", e);
        }
        return out.toByteArray();
    }
}
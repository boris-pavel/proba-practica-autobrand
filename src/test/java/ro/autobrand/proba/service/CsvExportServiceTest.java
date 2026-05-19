package ro.autobrand.proba.service;

import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    CsvExportService service = new CsvExportService();

    @Test
    void emits_utf8_bom_header_and_rows() {
        byte[] result = service.toCsv(List.of(
                InvoiceLineDto.builder()
                        .productCode("ABC123")
                        .productName("Filtru aer")
                        .unitPrice(new BigDecimal("25.50"))
                        .currency("RON")
                        .quantity(new BigDecimal("2"))
                        .build()
        ));

        String csv = new String(result, StandardCharsets.UTF_8);

        // BOM (3 bytes EF BB BF) ca să se deschidă corect în Excel cu diacritice
        assertThat(result[0]).isEqualTo((byte) 0xEF);
        assertThat(result[1]).isEqualTo((byte) 0xBB);
        assertThat(result[2]).isEqualTo((byte) 0xBF);

        assertThat(csv).contains("cod_produs,denumire,pret_unitar,moneda,cantitate");
        assertThat(csv).contains("ABC123,Filtru aer,25.50,RON,2");
    }

    @Test
    void emits_only_header_when_empty() {
        byte[] result = service.toCsv(List.of());
        String csv = new String(result, StandardCharsets.UTF_8);

        assertThat(csv).contains("cod_produs,denumire,pret_unitar,moneda,cantitate");
        // doar header + newline
        assertThat(csv.trim().split("\\r?\\n")).hasSize(1);
    }
}
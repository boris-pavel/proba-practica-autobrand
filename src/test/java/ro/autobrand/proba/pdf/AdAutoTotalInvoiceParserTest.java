package ro.autobrand.proba.pdf;

import org.junit.jupiter.api.Test;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdAutoTotalInvoiceParserTest {

    @Test
    void parses_sample_invoice() throws Exception {
        AdAutoTotalInvoiceParser parser = new AdAutoTotalInvoiceParser();
        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample-invoice.pdf")) {
            List<InvoiceLineDto> lines = parser.parse(in);

            assertThat(lines).hasSize(1);
            InvoiceLineDto first = lines.get(0);
            assertThat(first.getProductCode()).isEqualTo("172812F");
            assertThat(first.getProductName()).isEqualTo("COMUTATOR PORNIRE FEBI");
            assertThat(first.getUnitPrice()).isEqualByComparingTo("251.96");
            assertThat(first.getCurrency()).isEqualTo("RON");
            assertThat(first.getQuantity()).isEqualByComparingTo("-1");
        }
    }
}
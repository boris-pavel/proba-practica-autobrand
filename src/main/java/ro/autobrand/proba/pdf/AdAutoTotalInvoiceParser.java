package ro.autobrand.proba.pdf;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class AdAutoTotalInvoiceParser implements InvoiceParser {

    /** Anchor: "Identificator vanzator articol pentru linia 1 :172812F" */
    private static final Pattern ID_PATTERN =
            Pattern.compile("Identificator vanzator articol pentru linia (\\d+)\\s*:\\s*(\\S+)");

    @Override
    public List<InvoiceLineDto> parse(InputStream stream) throws IOException {
        byte[] bytes = stream.readAllBytes();
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(doc);
            log.debug("PDF text extracted, length={}", text.length());
            return extractLines(text);
        }
    }

    /** Package-private for testing. */
    List<InvoiceLineDto> extractLines(String text) {
        List<InvoiceLineDto> result = new ArrayList<>();

        // Pass 1: găsește toate produsele din ancorele "Identificator vanzator..."
        Matcher idMatcher = ID_PATTERN.matcher(text);
        while (idMatcher.find()) {
            int lineNum = Integer.parseInt(idMatcher.group(1));
            String code = idMatcher.group(2).trim();

            InvoiceLineDto line = extractDataRow(text, lineNum, code);
            if (line != null) {
                result.add(line);
            } else {
                log.warn("Could not match data row for line={} code={}", lineNum, code);
            }
        }
        log.info("Extracted {} invoice lines", result.size());
        return result;
    }

    /**
     * Construiește un regex care folosește codul produsului ca ancoră fixă —
     * elimină ambiguitatea boundary-ului între net_value și code (sunt lipite în PDF).
     *
     * Pattern data row:
     *   <unit_price> <currency> <qty_base> <qty_invoiced> <UM> <VAT%> <net_value><code> <name><line_num>
     * Exemplu:
     *   "251.96 RON -1 -1 H87 19 -251.96172812F COMUTATOR PORNIRE FEBI1"
     */
    private InvoiceLineDto extractDataRow(String text, int lineNum, String code) {
        Pattern dataPattern = Pattern.compile(
                "([\\d.]+)\\s+([A-Z]{3})\\s+(-?\\d+)\\s+(-?\\d+)\\s+\\S+\\s+\\d+\\s+(-?[\\d.]+)"
                        + Pattern.quote(code)
                        + "\\s+(.+?)\\s*" + lineNum + "\\s*$",
                Pattern.MULTILINE
        );
        Matcher m = dataPattern.matcher(text);
        if (!m.find()) return null;

        return InvoiceLineDto.builder()
                .productCode(code)
                .productName(m.group(6).trim())
                .unitPrice(new BigDecimal(m.group(1)))
                .currency(m.group(2))
                .quantity(new BigDecimal(m.group(4)))      // cantitate facturată (al doilea -1)
                .build();
    }
}
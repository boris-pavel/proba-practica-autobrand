package ro.autobrand.proba.pdf;

import ro.autobrand.proba.dto.InvoiceLineDto;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface InvoiceParser {
    List<InvoiceLineDto> parse(InputStream pdfStream) throws IOException;
}
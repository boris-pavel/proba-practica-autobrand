package ro.autobrand.proba.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

class PdfTextDumpTest {

    @Test
    void dump_text_from_sample() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/sample-invoice.pdf");
             PDDocument doc = Loader.loadPDF(IOUtils.toByteArray(in))) {
            String text = new PDFTextStripper().getText(doc);
            System.out.println("===== START PDF TEXT =====");
            System.out.println(text);
            System.out.println("===== END PDF TEXT =====");
        }
    }
}
package ro.autobrand.proba.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InvoiceLineDto {
    String productCode;
    String productName;
    BigDecimal unitPrice;
    String currency;
    BigDecimal quantity;
}
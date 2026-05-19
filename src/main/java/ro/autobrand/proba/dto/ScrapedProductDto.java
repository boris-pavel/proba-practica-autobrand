package ro.autobrand.proba.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class ScrapedProductDto {
    String name;
    String description;
    BigDecimal price;
    String currency;
    String imageUrl;
    String sourceUrl;
}
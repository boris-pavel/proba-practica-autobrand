package ro.autobrand.proba.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ro.autobrand.proba.model.Product;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {

    private Long id;

    @NotBlank(message = "Numele e obligatoriu")
    @Size(max = 255, message = "Numele are maxim 255 caractere")
    private String name;

    @Size(max = 5000, message = "Descrierea are maxim 5000 caractere")
    private String description;

    @NotNull(message = "Prețul e obligatoriu")
    @Positive(message = "Prețul trebuie să fie pozitiv")
    private BigDecimal price;

    @NotBlank(message = "Moneda e obligatorie")
    @Size(min = 3, max = 3, message = "Moneda trebuie să fie cod ISO de 3 litere")
    private String currency;

    @Size(max = 1000, message = "URL imagine prea lung")
    private String imageUrl;

    @Size(max = 1000, message = "URL sursă prea lung")
    private String sourceUrl;

    public static ProductDto from(Product p) {
        return ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .imageUrl(p.getImageUrl())
                .sourceUrl(p.getSourceUrl())
                .build();
    }

    public void applyTo(Product p) {
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCurrency(currency);
        p.setImageUrl(imageUrl);
        p.setSourceUrl(sourceUrl);
        p.setManuallyEdited(true);
    }
}
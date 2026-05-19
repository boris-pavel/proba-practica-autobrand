package ro.autobrand.proba.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.autobrand.proba.dto.ProductDto;
import ro.autobrand.proba.exception.ProductNotFoundException;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;
import ro.autobrand.proba.service.ProductService;
import ro.autobrand.proba.service.ExchangeRateService;


@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;
    private final ProductService productService;
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Model model,
            @RequestHeader(value = "HX-Request", required = false) String htmxHeader
    ) {
        Page<Product> page = productService.search(search, currency, minPrice, maxPrice, pageable);
        model.addAttribute("page", page);
        model.addAttribute("search", search);
        model.addAttribute("currency", currency);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);

        // Dacă request-ul vine de la HTMX (header HX-Request), returnăm doar fragment-ul tbody
        return htmxHeader != null ? "products/list :: products-tbody" : "products/list";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        model.addAttribute("productDto", ProductDto.from(p));
        model.addAttribute("product", p);
        return "products/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("productDto") ProductDto dto,
                         BindingResult bindingResult,
                         RedirectAttributes ra,
                         Model model) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        if (bindingResult.hasErrors()) {
            model.addAttribute("product", p);
            return "products/edit";
        }
        dto.applyTo(p);
        repository.save(p);
        exchangeRateService.recomputeRonFor(p);     // ← NOU: recalculează RON după edit
        ra.addFlashAttribute("success", "Produs actualizat");
        return "redirect:/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        repository.deleteById(id);
        ra.addFlashAttribute("success", "Produs șters");
        return "redirect:/products";
    }

    @PostMapping("/{id}/reset")
    public String reset(@PathVariable Long id, RedirectAttributes ra) {
        Product p = repository.findById(id).orElseThrow(() ->
                new ProductNotFoundException("Product not found: " + id));
        p.setManuallyEdited(false);
        repository.save(p);
        ra.addFlashAttribute("success",
                "Flag manually_edited resetat — la următorul scrape, produsul va fi rescris cu datele de pe site.");
        return "redirect:/products";
    }
}
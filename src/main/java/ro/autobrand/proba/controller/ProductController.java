package ro.autobrand.proba.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ro.autobrand.proba.model.Product;
import ro.autobrand.proba.repository.ProductRepository;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository repository;

    @GetMapping
    public String list(
            @PageableDefault(size = 20, sort = "name") Pageable pageable,
            Model model
    ) {
        Page<Product> page = repository.findAll(pageable);
        model.addAttribute("page", page);
        return "products/list";
    }
}
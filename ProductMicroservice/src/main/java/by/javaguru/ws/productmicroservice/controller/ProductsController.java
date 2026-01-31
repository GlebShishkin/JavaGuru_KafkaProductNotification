package by.javaguru.ws.productmicroservice.controller;

import by.javaguru.ws.productmicroservice.service.ProductService;
import by.javaguru.ws.productmicroservice.service.dto.CreateProductDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductsController {

    private final ProductService productService;

    public ProductsController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody CreateProductDto createProductDto) {

        String productId = productService.createProduct(createProductDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(productId);
    }
}

package by.javaguru.ws.productmicroservice.service;

import by.javaguru.ws.productmicroservice.service.dto.CreateProductDto;

public interface ProductService {
    String createProduct(CreateProductDto createProductDto);
}

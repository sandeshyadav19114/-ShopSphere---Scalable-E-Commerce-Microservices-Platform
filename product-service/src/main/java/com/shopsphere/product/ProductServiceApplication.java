package com.shopsphere.product;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
@SpringBootApplication @EnableCaching
public class ProductServiceApplication { public static void main(String[] a){ SpringApplication.run(ProductServiceApplication.class,a);} }

package com.shopsphere.cart;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication @EnableFeignClients
public class CartServiceApplication { public static void main(String[] a){ SpringApplication.run(CartServiceApplication.class,a);} }

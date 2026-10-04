package com.shopsphere.order;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
@SpringBootApplication @EnableFeignClients @org.springframework.scheduling.annotation.EnableScheduling
public class OrderServiceApplication { public static void main(String[] a){ SpringApplication.run(OrderServiceApplication.class,a);} }

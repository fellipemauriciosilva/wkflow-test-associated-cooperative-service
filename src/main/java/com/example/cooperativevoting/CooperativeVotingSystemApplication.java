package com.example.cooperativevoting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class CooperativeVotingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(CooperativeVotingSystemApplication.class, args);
    }
}

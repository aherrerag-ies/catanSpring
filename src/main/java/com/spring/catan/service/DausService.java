package com.spring.catan.service;

import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class DausService {
    private final Random aleatori = new Random();

    public int tirar() {
        return aleatori.nextInt(6) + 1;
    }
}

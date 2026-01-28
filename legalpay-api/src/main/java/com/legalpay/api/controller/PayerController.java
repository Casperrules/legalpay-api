package com.legalpay.api.controller;

import com.legalpay.domain.entity.Payer;
import com.legalpay.domain.repository.PayerRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payers")
public class PayerController {

    private final PayerRepository payerRepository;

    public PayerController(PayerRepository payerRepository) {
        this.payerRepository = payerRepository;
    }

    @GetMapping
    public List<Payer> list() {
        return payerRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Payer> getById(@PathVariable UUID id) {
        return payerRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

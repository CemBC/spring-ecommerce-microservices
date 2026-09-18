package com.ecommerce.saga_monitor.controller;

import com.ecommerce.saga_monitor.entity.SagaState;
import com.ecommerce.saga_monitor.repository.SagaStateRepository;
import com.ecommerce.saga_monitor.service.SagaCompensationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/sagas")
public class SagaController {

    private final SagaStateRepository repository;
    private final SagaCompensationService compensationService;

    public SagaController(
            SagaStateRepository repository,
            SagaCompensationService compensationService
    ) {
        this.repository = repository;
        this.compensationService = compensationService;
    }

    @GetMapping("/{orderId}")
    public SagaState get(
            @PathVariable Long orderId
    ) {
        return repository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Saga not found"
                        )
                );
    }

    @GetMapping
    public List<SagaState> getByStatus(
            @RequestParam(defaultValue = "IN_PROGRESS")
            String status
    ) {
        return repository
                .findByTerminalStatusOrderByUpdatedAtDesc(
                        status
                );
    }

    @PostMapping("/{orderId}/compensate")
    public SagaState compensate(
            @PathVariable Long orderId
    ) {
        return compensationService
                .compensate(orderId);
    }
}

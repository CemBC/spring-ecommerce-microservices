package com.ecommerce.saga_monitor.repository;

import com.ecommerce.saga_monitor.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SagaStateRepository
        extends JpaRepository<SagaState, Long> {

    List<SagaState>
    findByTerminalStatusAndUpdatedAtBefore(
            String terminalStatus,
            LocalDateTime cutoff
    );

    List<SagaState>
    findByTerminalStatusOrderByUpdatedAtDesc(
            String terminalStatus
    );
}

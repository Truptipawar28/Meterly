package com.meterly.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.UsageEvent;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    Optional<UsageEvent> findByIdempotencyKey(String idempotencyKey);
}

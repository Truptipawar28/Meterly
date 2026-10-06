package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.UsageEvent;

public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {
}

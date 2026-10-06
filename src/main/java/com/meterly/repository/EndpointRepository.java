package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.Endpoint;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
}

package com.meterly.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.ServiceEntity;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
}

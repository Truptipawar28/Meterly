package com.meterly.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.meterly.domain.ServiceEntity;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {

    // Derived query: Spring writes "where account_id = ? order by name" from the method name
    List<ServiceEntity> findByAccountIdOrderByName(Long accountId);
}

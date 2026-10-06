package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.meterly.domain.Endpoint;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    // "join fetch" loads the endpoint, its service and the account in ONE query,
    // instead of one query each when we walk endpoint -> service -> account.
    @Query("""
            select e from Endpoint e
            join fetch e.service s
            join fetch s.account
            where e.id = :id
            """)
    Optional<Endpoint> findWithServiceAndAccount(Long id);

    List<Endpoint> findByServiceIdOrderByName(Long serviceId);
}

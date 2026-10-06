package com.meterly.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.meterly.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("""
            select distinct i from Invoice i
            join fetch i.billingPeriod
            left join fetch i.lines
            where i.id = :id
            """)
    Optional<Invoice> findWithLines(Long id);

    @Query("""
            select i from Invoice i
            join fetch i.billingPeriod
            where i.account.id = :accountId
            order by i.issuedAt desc
            """)
    List<Invoice> findByAccount(Long accountId);
}

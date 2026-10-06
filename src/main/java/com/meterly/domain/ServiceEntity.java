package com.meterly.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * A product/service owned by an account.
 * Named ServiceEntity so it doesn't clash with Spring's @Service annotation.
 */
@Entity
@Table(name = "service")
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many services belong to one account. LAZY = load the account only when we use it.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 200)
    private String name;

    protected ServiceEntity() {
    }

    public ServiceEntity(Account account, String name) {
        this.account = account;
        this.name = name;
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public String getName() { return name; }
}

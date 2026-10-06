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

/** An API endpoint inside a service. Usage is recorded per endpoint. */
@Entity
@Table(name = "endpoint")
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false, length = 200)
    private String name;

    protected Endpoint() {
    }

    public Endpoint(ServiceEntity service, String name) {
        this.service = service;
        this.name = name;
    }

    public Long getId() { return id; }
    public ServiceEntity getService() { return service; }
    public String getName() { return name; }
}

package com.meterly.service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meterly.api.ApiException;
import com.meterly.domain.Account;
import com.meterly.domain.BillingPeriod;
import com.meterly.domain.Endpoint;
import com.meterly.domain.ServiceEntity;
import com.meterly.repository.AccountRepository;
import com.meterly.repository.BillingPeriodRepository;
import com.meterly.repository.EndpointRepository;
import com.meterly.repository.ServiceRepository;

/** Creates and lists the customer hierarchy: account -> service -> endpoint. */
@Service
public class CatalogService {

    private final AccountRepository accounts;
    private final ServiceRepository services;
    private final EndpointRepository endpoints;
    private final BillingPeriodRepository periods;

    // Constructor injection: Spring passes in the beans. No @Autowired needed with a single constructor.
    public CatalogService(AccountRepository accounts, ServiceRepository services,
                          EndpointRepository endpoints, BillingPeriodRepository periods) {
        this.accounts = accounts;
        this.services = services;
        this.endpoints = endpoints;
        this.periods = periods;
    }

    /** A new account starts with an OPEN billing period for the current month (UTC). */
    @Transactional
    public Account createAccount(String name, BigDecimal planIncluded) {
        return createAccount(name, planIncluded, YearMonth.now(ZoneOffset.UTC));
    }

    /** Same, but the first period starts in the given month (demo data uses last month). */
    @Transactional
    public Account createAccount(String name, BigDecimal planIncluded, YearMonth firstMonth) {
        Account account = accounts.save(new Account(name, planIncluded == null ? BigDecimal.ZERO : planIncluded));
        periods.save(new BillingPeriod(account, firstMonth.atDay(1), firstMonth.atEndOfMonth()));
        return account;
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accounts.findAll();
    }

    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accounts.findById(id).orElseThrow(() -> ApiException.notFound("Account", id));
    }

    @Transactional
    public ServiceEntity createService(Long accountId, String name) {
        return services.save(new ServiceEntity(getAccount(accountId), name));
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> listServices(Long accountId) {
        getAccount(accountId);
        return services.findByAccountIdOrderByName(accountId);
    }

    @Transactional
    public Endpoint createEndpoint(Long serviceId, String name) {
        ServiceEntity service = services.findById(serviceId)
                .orElseThrow(() -> ApiException.notFound("Service", serviceId));
        return endpoints.save(new Endpoint(service, name));
    }

    @Transactional(readOnly = true)
    public List<Endpoint> listEndpoints(Long serviceId) {
        if (!services.existsById(serviceId)) {
            throw ApiException.notFound("Service", serviceId);
        }
        return endpoints.findByServiceIdOrderByName(serviceId);
    }

    @Transactional(readOnly = true)
    public List<BillingPeriod> listPeriods(Long accountId) {
        getAccount(accountId);
        return periods.findByAccountIdOrderByStartsOnDesc(accountId);
    }
}

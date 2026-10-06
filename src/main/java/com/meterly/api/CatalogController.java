package com.meterly.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.api.dto.CatalogDtos.AccountResponse;
import com.meterly.api.dto.CatalogDtos.CreateAccountRequest;
import com.meterly.api.dto.CatalogDtos.CreateNamedRequest;
import com.meterly.api.dto.CatalogDtos.EndpointResponse;
import com.meterly.api.dto.CatalogDtos.PeriodResponse;
import com.meterly.api.dto.CatalogDtos.ServiceResponse;
import com.meterly.service.CatalogService;

import jakarta.validation.Valid;

/** REST API for the customer hierarchy. The controller only translates HTTP <-> service calls. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return AccountResponse.from(catalog.createAccount(request.name(), request.planIncluded()));
    }

    @GetMapping("/accounts")
    public List<AccountResponse> listAccounts() {
        return catalog.listAccounts().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/accounts/{accountId}")
    @PreAuthorize("@access.canRead(authentication, #accountId)")
    public AccountResponse getAccount(@PathVariable Long accountId) {
        return AccountResponse.from(catalog.getAccount(accountId));
    }

    @PostMapping("/accounts/{accountId}/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse createService(@PathVariable Long accountId, @Valid @RequestBody CreateNamedRequest request) {
        return ServiceResponse.from(catalog.createService(accountId, request.name()));
    }

    @GetMapping("/accounts/{accountId}/services")
    public List<ServiceResponse> listServices(@PathVariable Long accountId) {
        return catalog.listServices(accountId).stream().map(ServiceResponse::from).toList();
    }

    @PostMapping("/services/{serviceId}/endpoints")
    @ResponseStatus(HttpStatus.CREATED)
    public EndpointResponse createEndpoint(@PathVariable Long serviceId, @Valid @RequestBody CreateNamedRequest request) {
        return EndpointResponse.from(catalog.createEndpoint(serviceId, request.name()));
    }

    @GetMapping("/services/{serviceId}/endpoints")
    public List<EndpointResponse> listEndpoints(@PathVariable Long serviceId) {
        return catalog.listEndpoints(serviceId).stream().map(EndpointResponse::from).toList();
    }

    @GetMapping("/accounts/{accountId}/periods")
    public List<PeriodResponse> listPeriods(@PathVariable Long accountId) {
        return catalog.listPeriods(accountId).stream().map(PeriodResponse::from).toList();
    }
}

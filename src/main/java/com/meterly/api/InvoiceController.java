package com.meterly.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.api.dto.InvoiceDtos.InvoiceResponse;
import com.meterly.api.dto.InvoiceDtos.InvoiceSummary;
import com.meterly.domain.Invoice;
import com.meterly.security.AccountAccess;
import com.meterly.service.InvoiceService;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final InvoiceService invoices;
    private final AccountAccess access;

    public InvoiceController(InvoiceService invoices, AccountAccess access) {
        this.invoices = invoices;
        this.access = access;
    }

    /** Close the current period and issue its invoice. (ADMIN only, see SecurityConfig) */
    @PostMapping("/accounts/{accountId}/close-period")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse closePeriod(@PathVariable Long accountId) {
        return InvoiceResponse.from(invoices.closePeriod(accountId));
    }

    @GetMapping("/accounts/{accountId}/invoices")
    @PreAuthorize("@access.canRead(authentication, #accountId)")
    public List<InvoiceSummary> listInvoices(@PathVariable Long accountId) {
        return invoices.listInvoices(accountId).stream().map(InvoiceSummary::from).toList();
    }

    @GetMapping("/invoices/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable Long invoiceId) {
        Invoice invoice = invoices.getInvoice(invoiceId);
        access.check(invoice.getAccount().getId());
        return InvoiceResponse.from(invoice);
    }
}

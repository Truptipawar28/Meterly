package com.meterly.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meterly.api.dto.InvoiceDtos.InvoiceResponse;
import com.meterly.api.dto.InvoiceDtos.InvoiceSummary;
import com.meterly.service.InvoiceService;

@RestController
@RequestMapping("/api")
public class InvoiceController {

    private final InvoiceService invoices;

    public InvoiceController(InvoiceService invoices) {
        this.invoices = invoices;
    }

    /** Close the current period and issue its invoice. */
    @PostMapping("/accounts/{accountId}/close-period")
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse closePeriod(@PathVariable Long accountId) {
        return InvoiceResponse.from(invoices.closePeriod(accountId));
    }

    @GetMapping("/accounts/{accountId}/invoices")
    public List<InvoiceSummary> listInvoices(@PathVariable Long accountId) {
        return invoices.listInvoices(accountId).stream().map(InvoiceSummary::from).toList();
    }

    @GetMapping("/invoices/{invoiceId}")
    public InvoiceResponse getInvoice(@PathVariable Long invoiceId) {
        return InvoiceResponse.from(invoices.getInvoice(invoiceId));
    }
}

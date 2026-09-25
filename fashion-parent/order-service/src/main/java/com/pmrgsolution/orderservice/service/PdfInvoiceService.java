package com.pmrgsolution.orderservice.service;

import com.pmrgsolution.orderservice.entity.Order;

public interface PdfInvoiceService {
    byte[] generateInvoice(Order order);
}

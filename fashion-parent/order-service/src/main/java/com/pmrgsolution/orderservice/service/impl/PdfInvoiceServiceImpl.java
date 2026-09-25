package com.pmrgsolution.orderservice.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pmrgsolution.orderservice.entity.Order;
import com.pmrgsolution.orderservice.entity.OrderAddressSnapshot;
import com.pmrgsolution.orderservice.entity.OrderItem;
import com.pmrgsolution.orderservice.service.PdfInvoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfInvoiceServiceImpl implements PdfInvoiceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss")
            .withZone(ZoneId.of("UTC"));

    @Value("${app.store.name:Fashion Store}")
    private String storeName;

    @Value("${app.store.support-email:support@fashionstore.com}")
    private String supportEmail;

    @Override
    public byte[] generateInvoice(Order order) {
        log.info("Generating PDF invoice for order number: {}", order.getOrderNumber());

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.DARK_GRAY);
            Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.GRAY);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // 1. Header
            Paragraph header = new Paragraph(storeName.toUpperCase(), titleFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);

            Paragraph docType = new Paragraph("TAX INVOICE / BILL OF SUPPLY", subtitleFont);
            docType.setAlignment(Element.ALIGN_CENTER);
            docType.setSpacingAfter(15);
            document.add(docType);

            // 2. Invoice & Order Metadata Table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingAfter(15);

            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(PdfPCell.NO_BORDER);
            leftCell.addElement(new Paragraph("Order Number: " + order.getOrderNumber(), boldFont));
            leftCell.addElement(new Paragraph("Order Date: " + (order.getCreatedAt() != null ? DATE_FORMATTER.format(order.getCreatedAt()) : "N/A"), normalFont));
            leftCell.addElement(new Paragraph("Payment Method: " + order.getPaymentType(), normalFont));
            leftCell.addElement(new Paragraph("Order Status: " + order.getStatus(), normalFont));
            metaTable.addCell(leftCell);

            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(new Paragraph("Invoice Generated: " + DATE_FORMATTER.format(java.time.Instant.now()), smallFont));
            if (order.getPaidAt() != null) {
                rightCell.addElement(new Paragraph("Paid At: " + DATE_FORMATTER.format(order.getPaidAt()), smallFont));
            }
            if (order.getTrackingNumber() != null) {
                rightCell.addElement(new Paragraph("Tracking #: " + order.getTrackingNumber(), smallFont));
            }
            if (order.getCourierName() != null) {
                rightCell.addElement(new Paragraph("Courier: " + order.getCourierName(), smallFont));
            }
            metaTable.addCell(rightCell);

            document.add(metaTable);

            // 3. Shipping Address
            OrderAddressSnapshot addr = order.getAddressSnapshot();
            if (addr != null) {
                Paragraph addrHeading = new Paragraph("SHIPPING ADDRESS", sectionFont);
                addrHeading.setSpacingAfter(5);
                document.add(addrHeading);

                Paragraph addrContent = new Paragraph();
                addrContent.setFont(normalFont);
                addrContent.add(addr.getRecipientName() + " (Phone: " + addr.getRecipientPhone() + ")\n");
                addrContent.add(addr.getAddressLine1() + "\n");
                if (addr.getAddressLine2() != null && !addr.getAddressLine2().isBlank()) {
                    addrContent.add(addr.getAddressLine2() + "\n");
                }
                if (addr.getLandmark() != null && !addr.getLandmark().isBlank()) {
                    addrContent.add("Landmark: " + addr.getLandmark() + "\n");
                }
                addrContent.add(addr.getCity() + ", " + addr.getState() + " - " + addr.getPostalCode() + "\n");
                addrContent.add(addr.getCountry());
                addrContent.setSpacingAfter(15);
                document.add(addrContent);
            }

            // 4. Items Table
            Paragraph itemsHeading = new Paragraph("ORDER ITEMS", sectionFont);
            itemsHeading.setSpacingAfter(8);
            document.add(itemsHeading);

            PdfPTable itemsTable = new PdfPTable(6);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{0.6f, 3.5f, 1.5f, 1.2f, 0.8f, 1.4f});
            itemsTable.setSpacingAfter(15);

            // Table Headers
            String[] headers = {"#", "Item Description", "Size/Color", "Price", "Qty", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(new Color(240, 240, 240));
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                itemsTable.addCell(cell);
            }

            // Table Rows
            int index = 1;
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    PdfPCell numCell = new PdfPCell(new Phrase(String.valueOf(index++), normalFont));
                    numCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    numCell.setPadding(5);
                    itemsTable.addCell(numCell);

                    PdfPCell descCell = new PdfPCell(new Phrase(item.getProductTitle() + "\nSKU: " + item.getSku(), normalFont));
                    descCell.setPadding(5);
                    itemsTable.addCell(descCell);

                    String variant = (item.getVariantSize() != null ? item.getVariantSize() : "-")
                            + " / " + (item.getVariantColor() != null ? item.getVariantColor() : "-");
                    PdfPCell varCell = new PdfPCell(new Phrase(variant, normalFont));
                    varCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    varCell.setPadding(5);
                    itemsTable.addCell(varCell);

                    PdfPCell priceCell = new PdfPCell(new Phrase(String.format("₹%.2f", item.getUnitPrice()), normalFont));
                    priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    priceCell.setPadding(5);
                    itemsTable.addCell(priceCell);

                    PdfPCell qtyCell = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                    qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    qtyCell.setPadding(5);
                    itemsTable.addCell(qtyCell);

                    PdfPCell totCell = new PdfPCell(new Phrase(String.format("₹%.2f", item.getTotalPrice()), normalFont));
                    totCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    totCell.setPadding(5);
                    itemsTable.addCell(totCell);
                }
            }

            document.add(itemsTable);

            // 5. Price Breakdown Summary
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(50);
            summaryTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            summaryTable.setWidths(new float[]{2.5f, 1.5f});
            summaryTable.setSpacingAfter(20);

            addSummaryRow(summaryTable, "Subtotal:", order.getSubtotal(), normalFont);
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                addSummaryRow(summaryTable, "Coupon Discount (" + (order.getCouponCode() != null ? order.getCouponCode() : "") + "):",
                        order.getDiscountAmount().negate(), normalFont);
            }
            addSummaryRow(summaryTable, "Shipping Fee:", order.getShippingFee(), normalFont);
            addSummaryRow(summaryTable, "Tax Amount:", order.getTaxAmount(), normalFont);
            addSummaryRow(summaryTable, "Grand Total:", order.getTotalAmount(), boldFont);

            document.add(summaryTable);

            // 6. Footer
            Paragraph footer = new Paragraph();
            footer.setFont(smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.add("Thank you for shopping with " + storeName + "!\n");
            footer.add("For inquiries, support, or returns, please email: " + supportEmail + "\n");
            footer.add("This is a computer-generated invoice and does not require a physical signature.");
            document.add(footer);

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            log.error("Failed to generate PDF invoice for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate invoice PDF", e);
        }
    }

    private void addSummaryRow(PdfPTable table, String label, BigDecimal amount, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        labelCell.setPadding(3);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(amount != null ? String.format("₹%.2f", amount) : "₹0.00", font));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(3);
        table.addCell(valueCell);
    }
}

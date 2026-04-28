package com.stockhub.alert.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {


    private final JavaMailSender mailSender;

    // From email address from properties
    @Value("${app.alert.from-email}")
    private String fromEmail;

    // Admin email from properties
    @Value("${app.alert.admin-email}")
    private String adminEmail;

    // Send simple email
    public void sendEmail(
            String toEmail,
            String subject,
            String body) {

        try {
            SimpleMailMessage message =
                    new SimpleMailMessage();

            // Set sender
            message.setFrom(fromEmail);

            // Set recipient
            message.setTo(toEmail);

            // Set subject
            message.setSubject(subject);

            // Set body
            message.setText(body);

            // Send email
            mailSender.send(message);

            log.info("Email sent successfully to: {}",
                    toEmail);

        } catch (Exception e) {
            // Log error but do not crash
            // Alert still saved even if email fails
            log.error("Failed to send email to {}: {}",
                    toEmail, e.getMessage());
        }
    }

    // Send critical low stock email to admin
    public void sendLowStockEmail(
            Integer productId,
            String productName,
            Integer warehouseId,
            Integer currentQty,
            Integer reorderLevel) {

        String subject =
                "🚨 CRITICAL: Low Stock Alert - "
                        + productName;

        String body =
                "Dear Admin,\n\n"
                        + "This is an urgent notification "
                        + "from StockHub Inventory System.\n\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                        + "LOW STOCK ALERT DETAILS\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                        + "Product ID   : " + productId + "\n"
                        + "Product Name : " + productName + "\n"
                        + "Warehouse ID : " + warehouseId + "\n"
                        + "Current Qty  : " + currentQty + "\n"
                        + "Reorder Level: " + reorderLevel + "\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                        + "⚠️ Immediate action required!\n"
                        + "Please create a Purchase Order "
                        + "for this product.\n\n"
                        + "Login to StockHub to take action:\n"
                        + "http://localhost:4200\n\n"
                        + "Regards,\n"
                        + "StockHub System\n"
                        + "stockhub.connect@gmail.com\n"
                        + "📞 +91 8529126772";

        sendEmail(adminEmail, subject, body);
    }

    // Send overstock email
    public void sendOverstockEmail(
            Integer productId,
            String productName,
            Integer warehouseId,
            Integer currentQty,
            Integer maxStockLevel) {

        String subject =
                "⚠️ WARNING: Overstock Alert - "
                        + productName;

        String body =
                "Dear Admin,\n\n"
                        + "StockHub has detected an "
                        + "overstock situation.\n\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                        + "OVERSTOCK ALERT DETAILS\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n"
                        + "Product ID     : " + productId + "\n"
                        + "Product Name   : " + productName + "\n"
                        + "Warehouse ID   : " + warehouseId + "\n"
                        + "Current Qty    : " + currentQty + "\n"
                        + "Max Stock Level: " + maxStockLevel + "\n"
                        + "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n"
                        + "Please review stock levels.\n\n"
                        + "Regards,\n"
                        + "StockHub System";

        sendEmail(adminEmail, subject, body);
    }
}
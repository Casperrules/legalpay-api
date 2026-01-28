package com.legalpay.api.dto;

import java.math.BigDecimal;

public class PaymentOrderResponse {
    
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String razorpayKeyId;
    private String contractTitle;
    private String merchantName;
    private String payerEmail;
    private String payerPhone;
    
    public PaymentOrderResponse() {}
    
    public PaymentOrderResponse(String orderId, BigDecimal amount, String currency, 
                                String razorpayKeyId, String contractTitle, 
                                String merchantName, String payerEmail, String payerPhone) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.razorpayKeyId = razorpayKeyId;
        this.contractTitle = contractTitle;
        this.merchantName = merchantName;
        this.payerEmail = payerEmail;
        this.payerPhone = payerPhone;
    }
    
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    public void setRazorpayKeyId(String razorpayKeyId) {
        this.razorpayKeyId = razorpayKeyId;
    }

    public String getContractTitle() {
        return contractTitle;
    }

    public void setContractTitle(String contractTitle) {
        this.contractTitle = contractTitle;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getPayerEmail() {
        return payerEmail;
    }

    public void setPayerEmail(String payerEmail) {
        this.payerEmail = payerEmail;
    }

    public String getPayerPhone() {
        return payerPhone;
    }

    public void setPayerPhone(String payerPhone) {
        this.payerPhone = payerPhone;
    }
}

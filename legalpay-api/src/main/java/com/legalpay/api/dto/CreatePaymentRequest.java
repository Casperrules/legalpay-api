package com.legalpay.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class CreatePaymentRequest {
    
    @NotNull(message = "Contract ID is required")
    private UUID contractId;
    
    public UUID getContractId() {
        return contractId;
    }

    public void setContractId(UUID contractId) {
        this.contractId = contractId;
    }
}

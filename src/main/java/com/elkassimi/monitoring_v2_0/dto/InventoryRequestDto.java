package com.elkassimi.monitoring_v2_0.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/** sender.py: send_inventory(). */
@Data
public class InventoryRequestDto {

    @NotBlank
    private String agentId;

    private Double timestamp;

    private Map<String, Object> inventory;
}

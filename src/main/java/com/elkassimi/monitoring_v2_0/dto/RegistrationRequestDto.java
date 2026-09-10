package com.elkassimi.monitoring_v2_0.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Agent self-registration payload (sender.py: register_agent()). */
@Setter
@Getter
public class RegistrationRequestDto {

    @NotBlank
    private String agentId;

    private String hostname;
    private String os;

    @JsonProperty("os_version")
    private String osVersion;

    private String architecture;

    @JsonProperty("python_version")
    private String pythonVersion;

    /** Epoch seconds, as sent by platform.python_version()-side time.time(). */
    @JsonProperty("registered_at")
    private Double registeredAt;

}

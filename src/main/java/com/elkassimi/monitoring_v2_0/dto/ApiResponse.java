package com.elkassimi.monitoring_v2_0.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/** Standard API envelope so every response is consistent. */
@Data
public class ApiResponse {

    private String status;
    private Object data;
    private String message;

    public ApiResponse() {}

    public ApiResponse(String status, Object data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    public static ApiResponse ok(Object data) {
        return new ApiResponse("ok", data, null);
    }

    public static ApiResponse ok() {
        return new ApiResponse("ok", null, null);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse("error", null, message);
    }

}

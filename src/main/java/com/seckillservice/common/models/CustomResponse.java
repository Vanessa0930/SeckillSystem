package com.seckillservice.common.models;

public class CustomResponse {
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";

    private String status;
    private String message;

    public CustomResponse(String s, String msg) {
        this.status = s;
        this.message = msg;
    }

    public String getStatus() {
        return this.status;
    }
    public String getMessage() {
        return this.message;
    }
}

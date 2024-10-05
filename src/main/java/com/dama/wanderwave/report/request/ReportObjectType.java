package com.dama.wanderwave.report.request;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReportObjectType {
    COMMENT("comment"),
    POST("post"),
    USER("user");

    private final String value;

    ReportObjectType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
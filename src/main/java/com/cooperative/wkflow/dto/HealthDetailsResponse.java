package com.cooperative.wkflow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HealthDetailsResponse {
    @JsonProperty("name")
    private String name;

    @JsonProperty("version")
    private String version;

    public HealthDetailsResponse() {
    }

    public HealthDetailsResponse(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}

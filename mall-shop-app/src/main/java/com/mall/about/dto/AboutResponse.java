package com.mall.about.dto;

public class AboutResponse {

    private final String name;
    private final String description;
    private final String version;

    public AboutResponse(String name, String description, String version) {
        this.name = name;
        this.description = description;
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }
}

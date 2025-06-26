package com.bengebackend.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ThingDesc {
    @JsonProperty("名称")
    private List<String> name;

    @JsonProperty("描绘")
    private List<String> desc;

    // Getters and Setters
    public List<String> getName() {
        return name;
    }

    public void setName(List<String> name) {
        this.name = name;
    }

    public List<String> getDesc() {
        return desc;
    }

    public void setDesc(List<String> desc) {
        this.desc = desc;
    }
}

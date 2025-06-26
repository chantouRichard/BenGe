package com.bengebackend.entity;

public class ImageParameters {
    private String size = "1024*1024";
    private int n = 1;
    private boolean promptExtend = true;
    private boolean watermark = false;

    // Getters and setters
    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public boolean isPromptExtend() {
        return promptExtend;
    }

    public void setPromptExtend(boolean promptExtend) {
        this.promptExtend = promptExtend;
    }

    public boolean isWatermark() {
        return watermark;
    }

    public void setWatermark(boolean watermark) {
        this.watermark = watermark;
    }
}

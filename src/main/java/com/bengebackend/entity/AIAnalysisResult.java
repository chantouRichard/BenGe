package com.bengebackend.entity;

import java.util.List;

public class AIAnalysisResult {
    private List<String> point; // 亮点
    private List<String> difficulty; // 难点
    private List<String> suggestion; // 建议
    private int logicality = -1; // 逻辑性
    private int storiness = -1; // 故事性
    private int experience = -1; // 体验感

    public List<String> getPoint() {
        return point;
    }

    public void setPoint(List<String> point) {
        this.point = point;
    }

    public List<String> getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(List<String> difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(List<String> suggestion) {
        this.suggestion = suggestion;
    }

    public int getLogicality() {
        return logicality;
    }

    public void setLogicality(int logicality) {
        this.logicality = logicality;
    }

    public int getStoriness() {
        return storiness;
    }

    public void setStoriness(int storiness) {
        this.storiness = storiness;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }
}

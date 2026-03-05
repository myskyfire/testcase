package com.example.demo.agent;

import java.util.List;
import java.util.Map;

public class AgentSpec {
    private String name;
    private List<Step> steps;
    private Map<String, Object> defaults;
    private Integer globalRetryTimes;
    private Integer globalRetryDelayMs;
    private Integer globalTimeoutMs;
    private Integer breakerFailureThreshold;
    private Integer breakerOpenMs;
    private Integer breakerHalfOpenMaxCalls;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public Map<String, Object> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, Object> defaults) {
        this.defaults = defaults;
    }

    public Integer getGlobalRetryTimes() {
        return globalRetryTimes;
    }

    public void setGlobalRetryTimes(Integer globalRetryTimes) {
        this.globalRetryTimes = globalRetryTimes;
    }

    public Integer getGlobalRetryDelayMs() {
        return globalRetryDelayMs;
    }

    public void setGlobalRetryDelayMs(Integer globalRetryDelayMs) {
        this.globalRetryDelayMs = globalRetryDelayMs;
    }

    public Integer getGlobalTimeoutMs() {
        return globalTimeoutMs;
    }

    public void setGlobalTimeoutMs(Integer globalTimeoutMs) {
        this.globalTimeoutMs = globalTimeoutMs;
    }

    public Integer getBreakerFailureThreshold() {
        return breakerFailureThreshold;
    }

    public void setBreakerFailureThreshold(Integer breakerFailureThreshold) {
        this.breakerFailureThreshold = breakerFailureThreshold;
    }

    public Integer getBreakerOpenMs() {
        return breakerOpenMs;
    }

    public void setBreakerOpenMs(Integer breakerOpenMs) {
        this.breakerOpenMs = breakerOpenMs;
    }

    public Integer getBreakerHalfOpenMaxCalls() {
        return breakerHalfOpenMaxCalls;
    }

    public void setBreakerHalfOpenMaxCalls(Integer breakerHalfOpenMaxCalls) {
        this.breakerHalfOpenMaxCalls = breakerHalfOpenMaxCalls;
    }
}

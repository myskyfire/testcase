package com.example.demo.agent;

import java.util.Map;

public class Step {
    private String id;
    private String type;
    private RequestSpec request;
    private String assign;
    private String extractPath;
    private boolean continueOnError;
    private Map<String, Object> params;
    private String when;
    private String foreach;
    private String itemVar;
    private String indexVar;
    private Integer retryTimes;
    private Integer retryDelayMs;
    private String switchExpr;
    private Map<String, java.util.List<Step>> cases;
    private java.util.List<Step> defaultSteps;
    private java.util.List<ParallelBranch> parallel;
    private Integer timeoutMs;
    private Integer failureThreshold;
    private Integer openMs;
    private java.util.List<Step> steps;
    private Integer backoffInitialDelayMs;
    private Double backoffMultiplier;
    private Integer backoffMaxDelayMs;
    private String assignPath;
    private Integer parallelMaxThreads;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RequestSpec getRequest() {
        return request;
    }

    public void setRequest(RequestSpec request) {
        this.request = request;
    }

    public String getAssign() {
        return assign;
    }

    public void setAssign(String assign) {
        this.assign = assign;
    }

    public String getExtractPath() {
        return extractPath;
    }

    public void setExtractPath(String extractPath) {
        this.extractPath = extractPath;
    }

    public boolean isContinueOnError() {
        return continueOnError;
    }

    public void setContinueOnError(boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getWhen() {
        return when;
    }

    public void setWhen(String when) {
        this.when = when;
    }

    public String getForeach() {
        return foreach;
    }

    public void setForeach(String foreach) {
        this.foreach = foreach;
    }

    public String getItemVar() {
        return itemVar;
    }

    public void setItemVar(String itemVar) {
        this.itemVar = itemVar;
    }

    public String getIndexVar() {
        return indexVar;
    }

    public void setIndexVar(String indexVar) {
        this.indexVar = indexVar;
    }

    public Integer getRetryTimes() {
        return retryTimes;
    }

    public void setRetryTimes(Integer retryTimes) {
        this.retryTimes = retryTimes;
    }

    public Integer getRetryDelayMs() {
        return retryDelayMs;
    }

    public void setRetryDelayMs(Integer retryDelayMs) {
        this.retryDelayMs = retryDelayMs;
    }

    public String getSwitchExpr() {
        return switchExpr;
    }

    public void setSwitchExpr(String switchExpr) {
        this.switchExpr = switchExpr;
    }

    public Map<String, java.util.List<Step>> getCases() {
        return cases;
    }

    public void setCases(Map<String, java.util.List<Step>> cases) {
        this.cases = cases;
    }

    public java.util.List<Step> getDefaultSteps() {
        return defaultSteps;
    }

    public void setDefaultSteps(java.util.List<Step> defaultSteps) {
        this.defaultSteps = defaultSteps;
    }

    public java.util.List<ParallelBranch> getParallel() {
        return parallel;
    }

    public void setParallel(java.util.List<ParallelBranch> parallel) {
        this.parallel = parallel;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getFailureThreshold() {
        return failureThreshold;
    }

    public void setFailureThreshold(Integer failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public Integer getOpenMs() {
        return openMs;
    }

    public void setOpenMs(Integer openMs) {
        this.openMs = openMs;
    }

    public java.util.List<Step> getSteps() {
        return steps;
    }

    public void setSteps(java.util.List<Step> steps) {
        this.steps = steps;
    }

    public Integer getBackoffInitialDelayMs() {
        return backoffInitialDelayMs;
    }

    public void setBackoffInitialDelayMs(Integer backoffInitialDelayMs) {
        this.backoffInitialDelayMs = backoffInitialDelayMs;
    }

    public Double getBackoffMultiplier() {
        return backoffMultiplier;
    }

    public void setBackoffMultiplier(Double backoffMultiplier) {
        this.backoffMultiplier = backoffMultiplier;
    }

    public Integer getBackoffMaxDelayMs() {
        return backoffMaxDelayMs;
    }

    public void setBackoffMaxDelayMs(Integer backoffMaxDelayMs) {
        this.backoffMaxDelayMs = backoffMaxDelayMs;
    }

    public String getAssignPath() {
        return assignPath;
    }

    public void setAssignPath(String assignPath) {
        this.assignPath = assignPath;
    }

    public Integer getParallelMaxThreads() {
        return parallelMaxThreads;
    }

    public void setParallelMaxThreads(Integer parallelMaxThreads) {
        this.parallelMaxThreads = parallelMaxThreads;
    }
}

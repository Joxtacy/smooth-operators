package com.smoothoperators.api.exception;

public class BusinessRuleException extends RuntimeException {
    private String ruleType;
    private String resourceId;
    
    public BusinessRuleException(String message) {
        super(message);
    }
    
    public BusinessRuleException(String message, String ruleType) {
        super(message);
        this.ruleType = ruleType;
    }
    
    public BusinessRuleException(String message, String ruleType, String resourceId) {
        super(message);
        this.ruleType = ruleType;
        this.resourceId = resourceId;
    }
    
    public String getRuleType() {
        return ruleType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
}
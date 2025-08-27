package com.smoothoperators.api.exception;

public class ResourceConflictException extends RuntimeException {
    private String resourceType;
    private String resourceId;
    private String conflictReason;
    
    public ResourceConflictException(String message) {
        super(message);
    }
    
    public ResourceConflictException(String message, String resourceType, String resourceId) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }
    
    public ResourceConflictException(String message, String resourceType, String resourceId, String conflictReason) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.conflictReason = conflictReason;
    }
    
    public String getResourceType() {
        return resourceType;
    }
    
    public String getResourceId() {
        return resourceId;
    }
    
    public String getConflictReason() {
        return conflictReason;
    }
}
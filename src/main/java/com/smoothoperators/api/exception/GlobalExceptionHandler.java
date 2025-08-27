package com.smoothoperators.api.exception;

import com.smoothoperators.api.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @Value("${api.documentation.base-url:https://docs.smoothoperators.com}")
    private String documentationBaseUrl;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException ex, 
            HttpServletRequest request) {
        
        BindingResult bindingResult = ex.getBindingResult();
        List<ErrorResponse.FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(fieldError -> new ErrorResponse.FieldError(
                fieldError.getField(),
                fieldError.getRejectedValue(),
                fieldError.getDefaultMessage(),
                fieldError.getCode()
            ))
            .collect(Collectors.toList());

        String details = String.format("Validation failed for %d field(s). Please review the field_errors for specific issues.", 
                                     fieldErrors.size());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Request validation failed",
            details,
            "Please check the required fields and their formats. Refer to the API documentation for valid field values.",
            LocalDateTime.now()
        );
        
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/validation-rules");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("total_errors", fieldErrors.size());
        additionalInfo.put("request_method", request.getMethod());
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolations(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        
        List<ErrorResponse.FieldError> fieldErrors = ex.getConstraintViolations().stream()
            .map(violation -> new ErrorResponse.FieldError(
                violation.getPropertyPath().toString(),
                violation.getInvalidValue(),
                violation.getMessage(),
                "CONSTRAINT_VIOLATION"
            ))
            .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
            "CONSTRAINT_VIOLATION",
            "Request contains invalid values",
            "One or more request parameters violate business constraints",
            "Please verify that all parameter values meet the specified constraints",
            LocalDateTime.now()
        );
        
        errorResponse.setFieldErrors(fieldErrors);
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/parameter-constraints");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, 
            HttpServletRequest request) {
        
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        Object value = ex.getValue();
        
        String details = String.format("Parameter '%s' with value '%s' cannot be converted to required type '%s'", 
                                     paramName, value, requiredType);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_PARAMETER",
            "Invalid parameter type",
            details,
            String.format("Please provide a valid %s value for parameter '%s'", requiredType.toLowerCase(), paramName),
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/parameter-types");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("parameter_name", paramName);
        additionalInfo.put("provided_value", value);
        additionalInfo.put("expected_type", requiredType);
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(OperatorNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOperatorNotFound(
            OperatorNotFoundException ex, 
            HttpServletRequest request) {
        
        String resourceId = extractResourceIdFromPath(request.getRequestURI());
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RESOURCE_NOT_FOUND", 
            "Operator not found",
            String.format("Operator with ID '%s' does not exist or has been deleted", resourceId),
            "Please verify the operator ID and ensure it exists. Use GET /api/v1/operators to list available operators.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/operators");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("resource_type", "Operator");
        additionalInfo.put("resource_id", resourceId);
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(
            UnauthorizedException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            "AUTH_FAILED",
            "Authentication failed", 
            "The request lacks valid authentication credentials or the provided credentials are invalid",
            "Please provide a valid API key in the Authorization header. Contact support if you need assistance with authentication.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/authentication");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("auth_type_required", "Bearer Token or API Key");
        additionalInfo.put("header_name", "Authorization");
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(
            ForbiddenException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INSUFFICIENT_PERMISSIONS",
            "Access denied",
            "You do not have sufficient permissions to perform this operation on the requested resource",
            "Please contact your administrator to request the necessary permissions, or use an account with appropriate access level.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/permissions");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("required_permission", getRequiredPermission(request.getMethod(), request.getRequestURI()));
        additionalInfo.put("resource_type", "Operator");
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(
            BusinessRuleException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            "BUSINESS_RULE_VIOLATION",
            "Business rule violation",
            ex.getMessage() != null ? ex.getMessage() : "The requested operation violates a business rule",
            "Please review the business constraints and modify your request accordingly. Check the operator's current state and any dependencies.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/business-rules");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("rule_type", ex.getRuleType() != null ? ex.getRuleType() : "GENERAL");
        if (ex.getResourceId() != null) {
            additionalInfo.put("resource_id", ex.getResourceId());
        }
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ErrorResponse> handleResourceConflict(
            ResourceConflictException ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            "RESOURCE_CONFLICT",
            "Resource conflict detected",
            ex.getMessage() != null ? ex.getMessage() : "The resource is currently being modified by another user or process",
            "Please wait a moment and try again. If the issue persists, refresh the resource data before attempting the operation.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/concurrency");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("conflict_type", "CONCURRENT_MODIFICATION");
        additionalInfo.put("retry_after_seconds", 30);
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceeded(
            MaxUploadSizeExceededException ex, 
            HttpServletRequest request) {
        
        long maxSize = ex.getMaxUploadSize();
        String maxSizeReadable = formatBytes(maxSize);
        
        ErrorResponse errorResponse = new ErrorResponse(
            "REQUEST_TOO_LARGE",
            "Request payload too large",
            String.format("The uploaded file or request body exceeds the maximum allowed size of %s", maxSizeReadable),
            String.format("Please reduce the file size to under %s or split large requests into smaller batches", maxSizeReadable),
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/api/upload-limits");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("max_upload_size_bytes", maxSize);
        additionalInfo.put("max_upload_size_readable", maxSizeReadable);
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericError(
            Exception ex, 
            HttpServletRequest request) {
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            "The server encountered an unexpected condition that prevented it from fulfilling the request",
            "Please try again later. If the problem persists, contact support with the correlation ID.",
            LocalDateTime.now()
        );
        
        errorResponse.setPath(request.getRequestURI());
        errorResponse.setCorrelationId(UUID.randomUUID().toString());
        errorResponse.setDocumentationUrl(documentationBaseUrl + "/support");
        
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("support_contact", "support@smoothoperators.com");
        additionalInfo.put("status_page", "https://status.smoothoperators.com");
        errorResponse.setAdditionalInfo(additionalInfo);
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    private String extractResourceIdFromPath(String path) {
        String[] segments = path.split("/");
        for (int i = 0; i < segments.length; i++) {
            if ("operators".equals(segments[i]) && i + 1 < segments.length) {
                try {
                    Long.parseLong(segments[i + 1]);
                    return segments[i + 1];
                } catch (NumberFormatException e) {
                    // Not a numeric ID, continue searching
                }
            }
        }
        return "unknown";
    }

    private String getRequiredPermission(String method, String path) {
        if (path.contains("/operators")) {
            switch (method.toUpperCase()) {
                case "GET": return "operators:read";
                case "POST": return "operators:create";
                case "PUT":
                case "PATCH": return "operators:update";
                case "DELETE": return "operators:delete";
                default: return "operators:read";
            }
        }
        return "unknown";
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
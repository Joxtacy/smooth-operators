package middleware

import (
	"encoding/json"
	"net/http"
	"strings"
	"time"
)

type AuthErrorResponse struct {
	Error     string `json:"error"`
	Message   string `json:"message"`
	Code      int    `json:"code"`
	Timestamp string `json:"timestamp"`
}

// List of valid API tokens (in production, this should be in a database or external service)
var validTokens = map[string]bool{
	"valid-api-token-123":    true,
	"another-valid-token":   true,
	"development-token-456": true,
}

func writeAuthError(w http.ResponseWriter, statusCode int, errorMsg, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("WWW-Authenticate", `Bearer realm="API"`)
	w.WriteHeader(statusCode)
	errorResp := AuthErrorResponse{
		Error:     errorMsg,
		Message:   message,
		Code:      statusCode,
		Timestamp: time.Now().Format(time.RFC3339),
	}
	json.NewEncoder(w).Encode(errorResp)
}

// Authentication middleware with improved validation
func AuthMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Check for Authorization header
		authHeader := r.Header.Get("Authorization")
		if authHeader == "" {
			writeAuthError(w, http.StatusUnauthorized, "Missing Authorization", "Authorization header is required. Use format: Authorization: Bearer <token>")
			return
		}

		// Validate Bearer token format
		if !strings.HasPrefix(authHeader, "Bearer ") {
			writeAuthError(w, http.StatusUnauthorized, "Invalid Authorization Format", "Authorization header must use Bearer token format: Authorization: Bearer <token>")
			return
		}

		// Extract token
		token := strings.TrimSpace(strings.TrimPrefix(authHeader, "Bearer "))
		if token == "" {
			writeAuthError(w, http.StatusUnauthorized, "Empty Token", "Bearer token cannot be empty")
			return
		}

		// Validate token length (basic security check)
		if len(token) < 10 {
			writeAuthError(w, http.StatusUnauthorized, "Invalid Token", "Token is too short")
			return
		}

		// Check if token is valid (in production, verify with auth service/database)
		if !validTokens[token] {
			writeAuthError(w, http.StatusUnauthorized, "Invalid Token", "The provided token is not valid or has expired")
			return
		}

		// Token is valid, add user context if needed
		// In a real implementation, you would:
		// 1. Decode JWT tokens
		// 2. Verify token signature
		// 3. Check expiration
		// 4. Add user information to request context

		// Continue to the next handler
		next.ServeHTTP(w, r)
	})
}

// Optional: Rate limiting middleware to prevent brute force attacks
func RateLimitMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Basic rate limiting could be implemented here
		// For production, use Redis or similar for distributed rate limiting
		next.ServeHTTP(w, r)
	})
}

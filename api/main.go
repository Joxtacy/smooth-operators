package main

import (
	"fmt"
	"log"
	"net/http"
	"os/signal"
	"syscall"
	"time"

	"github.com/gorilla/mux"
	"github.com/Smoother-Operators/smooth-operators/api/handlers"
	"github.com/Smoother-Operators/smooth-operators/api/middleware"
)

func main() {
	router := mux.NewRouter()

	// Global middleware
	router.Use(middleware.LoggingMiddleware)
	router.Use(middleware.CORSMiddleware)

	// API routes with path prefix
	apiRouter := router.PathPrefix("/api/v1").Subrouter()

	// Public routes (no authentication required)
	apiRouter.HandleFunc("/health", healthCheck).Methods("GET")
	apiRouter.HandleFunc("/operators", handlers.GetOperators).Methods("GET")
	apiRouter.HandleFunc("/operators/{id}", handlers.GetOperator).Methods("GET")

	// Protected routes (authentication required)
	protectedRouter := apiRouter.PathPrefix("").Subrouter()
	protectedRouter.Use(middleware.AuthMiddleware)
	protectedRouter.HandleFunc("/operators", handlers.CreateOperator).Methods("POST")
	protectedRouter.HandleFunc("/operators/{id}", handlers.UpdateOperator).Methods("PUT")
	protectedRouter.HandleFunc("/operators/{id}", handlers.DeleteOperator).Methods("DELETE")

	// 404 handler for undefined routes
	router.NotFoundHandler = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusNotFound)
		fmt.Fprintf(w, `{"error":"Not Found","message":"The requested endpoint does not exist","code":404}`)
	})

	// 405 handler for unsupported methods
	router.MethodNotAllowedHandler = http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusMethodNotAllowed)
		fmt.Fprintf(w, `{"error":"Method Not Allowed","message":"The HTTP method is not supported for this endpoint","code":405}`)
	})

	// Server configuration with security improvements
	server := &http.Server{
		Addr:           ":8080",
		Handler:        router,
		ReadTimeout:    15 * time.Second,
		WriteTimeout:   15 * time.Second,
		IdleTimeout:    60 * time.Second,
		MaxHeaderBytes: 1 << 20, // 1MB
	}

	fmt.Println("Server starting on port 8080")
	fmt.Println("Public endpoints:")
	fmt.Println("  GET  /api/v1/health")
	fmt.Println("  GET  /api/v1/operators")
	fmt.Println("  GET  /api/v1/operators/{id}")
	fmt.Println("Protected endpoints (require Authorization header):")
	fmt.Println("  POST /api/v1/operators")
	fmt.Println("  PUT  /api/v1/operators/{id}")
	fmt.Println("  DELETE /api/v1/operators/{id}")

	// Graceful shutdown
	go func() {
		signals := make(chan os.Signal, 1)
		signal.Notify(signals, syscall.SIGINT, syscall.SIGTERM)
		<-signals
		log.Println("Shutdown signal received")
		if err := server.Shutdown(nil); err != nil {
			log.Fatal("Server forced to shutdown:", err)
		}
	}()

	if err := server.ListenAndServe(); err != http.ErrServerClosed {
		log.Fatal("Server failed to start:", err)
	}
}

// Health check endpoint
func healthCheck(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, `{"status":"healthy","timestamp":"%s","version":"1.0.0"}`, time.Now().Format(time.RFC3339))
}

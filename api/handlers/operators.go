package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
	"strings"
	"sync"

	"github.com/gorilla/mux"
)

type Operator struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
	Role string `json:"role"`
}

type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message,omitempty"`
	Code    int    `json:"code"`
}

type ValidationError struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

type ValidationErrorResponse struct {
	Error   string            `json:"error"`
	Message string            `json:"message"`
	Code    int               `json:"code"`
	Errors  []ValidationError `json:"validation_errors"`
}

var (
	operators = []Operator{
		{ID: 1, Name: "John", Role: "Senior Operator"},
		{ID: 2, Name: "Jane", Role: "Junior Operator"},
		{ID: 3, Name: "Bob", Role: "Lead Operator"},
	}
	operatorsMutex sync.RWMutex
	nextID         = 4
)

// Valid roles for operators
var validRoles = map[string]bool{
	"Junior Operator": true,
	"Senior Operator": true,
	"Lead Operator":   true,
	"Manager":         true,
}

func writeJSONError(w http.ResponseWriter, statusCode int, errorMsg, message string) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	errorResp := ErrorResponse{
		Error:   errorMsg,
		Message: message,
		Code:    statusCode,
	}
	json.NewEncoder(w).Encode(errorResp)
}

func writeValidationError(w http.ResponseWriter, validationErrors []ValidationError) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusUnprocessableEntity)
	errorResp := ValidationErrorResponse{
		Error:   "Validation Failed",
		Message: "The request contains invalid data",
		Code:    http.StatusUnprocessableEntity,
		Errors:  validationErrors,
	}
	json.NewEncoder(w).Encode(errorResp)
}

func validateOperator(op *Operator, isUpdate bool) []ValidationError {
	var errors []ValidationError

	// Validate Name
	if strings.TrimSpace(op.Name) == "" {
		errors = append(errors, ValidationError{
			Field:   "name",
			Message: "Name is required and cannot be empty",
		})
	} else if len(op.Name) > 100 {
		errors = append(errors, ValidationError{
			Field:   "name",
			Message: "Name cannot exceed 100 characters",
		})
	}

	// Validate Role
	if strings.TrimSpace(op.Role) == "" {
		errors = append(errors, ValidationError{
			Field:   "role",
			Message: "Role is required and cannot be empty",
		})
	} else if !validRoles[op.Role] {
		validRolesList := []string{}
		for role := range validRoles {
			validRolesList = append(validRolesList, role)
		}
		errors = append(errors, ValidationError{
			Field:   "role",
			Message: fmt.Sprintf("Invalid role. Valid roles are: %s", strings.Join(validRolesList, ", ")),
		})
	}

	// For updates, validate ID is not being changed
	if isUpdate && op.ID != 0 {
		errors = append(errors, ValidationError{
			Field:   "id",
			Message: "ID cannot be modified during update",
		})
	}

	return errors
}

func GetOperators(w http.ResponseWriter, r *http.Request) {
	operatorsMutex.RLock()
	defer operatorsMutex.RUnlock()

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(operators)
}

func GetOperator(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil || id <= 0 {
		writeJSONError(w, http.StatusBadRequest, "Invalid ID", "ID must be a positive integer")
		return
	}

	operatorsMutex.RLock()
	defer operatorsMutex.RUnlock()

	for _, op := range operators {
		if op.ID == id {
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(op)
			return
		}
	}

	writeJSONError(w, http.StatusNotFound, "Operator Not Found", fmt.Sprintf("Operator with ID %d does not exist", id))
}

func CreateOperator(w http.ResponseWriter, r *http.Request) {
	// Validate Content-Type
	contentType := r.Header.Get("Content-Type")
	if !strings.HasPrefix(contentType, "application/json") {
		writeJSONError(w, http.StatusUnsupportedMediaType, "Invalid Content-Type", "Content-Type must be application/json")
		return
	}

	// Limit request body size to prevent abuse
	r.Body = http.MaxBytesReader(w, r.Body, 1048576) // 1MB limit

	var op Operator
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields() // Reject unknown fields

	if err := decoder.Decode(&op); err != nil {
		if strings.Contains(err.Error(), "unknown field") {
			writeJSONError(w, http.StatusBadRequest, "Invalid JSON", "Request contains unknown fields")
		} else {
			writeJSONError(w, http.StatusBadRequest, "Invalid JSON", "Request body contains invalid JSON")
		}
		return
	}

	// Validate input
	if validationErrors := validateOperator(&op, false); len(validationErrors) > 0 {
		writeValidationError(w, validationErrors)
		return
	}

	operatorsMutex.Lock()
	defer operatorsMutex.Unlock()

	// Check for duplicate name
	for _, existing := range operators {
		if strings.EqualFold(existing.Name, op.Name) {
			writeJSONError(w, http.StatusConflict, "Duplicate Operator", fmt.Sprintf("An operator with name '%s' already exists", op.Name))
			return
		}
	}

	op.ID = nextID
	nextID++
	operators = append(operators, op)

	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Location", fmt.Sprintf("/api/v1/operators/%d", op.ID))
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(op)
}

func UpdateOperator(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil || id <= 0 {
		writeJSONError(w, http.StatusBadRequest, "Invalid ID", "ID must be a positive integer")
		return
	}

	// Validate Content-Type
	contentType := r.Header.Get("Content-Type")
	if !strings.HasPrefix(contentType, "application/json") {
		writeJSONError(w, http.StatusUnsupportedMediaType, "Invalid Content-Type", "Content-Type must be application/json")
		return
	}

	// Limit request body size
	r.Body = http.MaxBytesReader(w, r.Body, 1048576) // 1MB limit

	var op Operator
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()

	if err := decoder.Decode(&op); err != nil {
		if strings.Contains(err.Error(), "unknown field") {
			writeJSONError(w, http.StatusBadRequest, "Invalid JSON", "Request contains unknown fields")
		} else {
			writeJSONError(w, http.StatusBadRequest, "Invalid JSON", "Request body contains invalid JSON")
		}
		return
	}

	// Validate input
	if validationErrors := validateOperator(&op, true); len(validationErrors) > 0 {
		writeValidationError(w, validationErrors)
		return
	}

	operatorsMutex.Lock()
	defer operatorsMutex.Unlock()

	// Find operator to update
	for i, operator := range operators {
		if operator.ID == id {
			// Check for duplicate name (excluding current operator)
			for j, existing := range operators {
				if j != i && strings.EqualFold(existing.Name, op.Name) {
					writeJSONError(w, http.StatusConflict, "Duplicate Operator", fmt.Sprintf("An operator with name '%s' already exists", op.Name))
					return
				}
			}

			op.ID = id
			operators[i] = op
			w.Header().Set("Content-Type", "application/json")
			json.NewEncoder(w).Encode(op)
			return
		}
	}

	writeJSONError(w, http.StatusNotFound, "Operator Not Found", fmt.Sprintf("Operator with ID %d does not exist", id))
}

func DeleteOperator(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, err := strconv.Atoi(idStr)
	if err != nil || id <= 0 {
		writeJSONError(w, http.StatusBadRequest, "Invalid ID", "ID must be a positive integer")
		return
	}

	operatorsMutex.Lock()
	defer operatorsMutex.Unlock()

	for i, op := range operators {
		if op.ID == id {
			operators = append(operators[:i], operators[i+1:]...)
			w.WriteHeader(http.StatusNoContent)
			return
		}
	}

	writeJSONError(w, http.StatusNotFound, "Operator Not Found", fmt.Sprintf("Operator with ID %d does not exist", id))
}

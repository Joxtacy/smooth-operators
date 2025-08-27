from flask import Blueprint, request, jsonify, current_app
from app.models import Operator
from app.utils import (
    validate_operator_data, 
    validate_operator_id, 
    sanitize_input,
    require_auth, 
    create_error_response
)
import logging

operators_bp = Blueprint('operators', __name__)

@operators_bp.route('/api/v1/operators', methods=['GET'])
def get_operators():
    """Get all operators"""
    try:
        operators = Operator.get_all()
        return jsonify({
            "data": [op.to_dict() for op in operators],
            "count": len(operators)
        }), 200
    except Exception as e:
        current_app.logger.error(f"Error getting operators: {e}")
        return create_error_response(
            "Failed to retrieve operators",
            500,
            "Please try again later or contact support",
            str(e)
        )

@operators_bp.route('/api/v1/operators/<operator_id>', methods=['GET'])
def get_operator(operator_id):
    """Get a specific operator by ID"""
    # Validate operator ID format
    validation_error = validate_operator_id(operator_id)
    if validation_error:
        return create_error_response(
            validation_error,
            400,
            "Operator ID must be a valid UUID (e.g., '550e8400-e29b-41d4-a716-446655440000')"
        )
    
    try:
        operator = Operator.get_by_id(operator_id)
        if not operator:
            return create_error_response(
                "Operator not found",
                404,
                f"No operator exists with ID '{operator_id}'. Please check the ID and try again."
            )
        return jsonify(operator.to_dict()), 200
    except Exception as e:
        current_app.logger.error(f"Error getting operator {operator_id}: {e}")
        return create_error_response(
            "Failed to retrieve operator",
            500,
            "Please try again later or contact support",
            str(e)
        )

@operators_bp.route('/api/v1/operators', methods=['POST'])
@require_auth
def create_operator():
    """Create a new operator"""
    try:
        # Check if request contains JSON data
        if not request.is_json:
            return create_error_response(
                "Request must contain JSON data",
                400,
                "Set Content-Type header to 'application/json' and provide valid JSON in request body"
            )
        
        data = request.get_json()
        if not data:
            return create_error_response(
                "Request body is empty or invalid JSON",
                400,
                "Provide valid JSON data with required fields: name, email"
            )
        
        # Sanitize input data
        data = sanitize_input(data)
        
        # Comprehensive validation
        validation_errors = validate_operator_data(data)
        if validation_errors:
            return create_error_response(
                "Validation failed",
                400,
                "Please fix the following issues with your request",
                validation_errors
            )
        
        # Check if operator with same email already exists
        existing_operator = Operator.get_by_email(data['email'])
        if existing_operator:
            return create_error_response(
                "Email address already in use",
                409,
                f"An operator with email '{data['email']}' already exists"
            )
        
        operator = Operator.create(data)
        return jsonify({
            "message": "Operator created successfully",
            "data": operator.to_dict()
        }), 201
        
    except Exception as e:
        current_app.logger.error(f"Error creating operator: {e}")
        return create_error_response(
            "Failed to create operator",
            500,
            "Please try again later or contact support",
            str(e)
        )

@operators_bp.route('/api/v1/operators/<operator_id>', methods=['PUT'])
@require_auth
def update_operator(operator_id):
    """Update an existing operator"""
    # Validate operator ID format
    validation_error = validate_operator_id(operator_id)
    if validation_error:
        return create_error_response(
            validation_error,
            400,
            "Operator ID must be a valid UUID"
        )
    
    try:
        # Check if request contains JSON data
        if not request.is_json:
            return create_error_response(
                "Request must contain JSON data",
                400,
                "Set Content-Type header to 'application/json' and provide valid JSON in request body"
            )
        
        data = request.get_json()
        if not data:
            return create_error_response(
                "Request body is empty or invalid JSON",
                400,
                "Provide valid JSON data with fields to update"
            )
        
        # Check if operator exists
        operator = Operator.get_by_id(operator_id)
        if not operator:
            return create_error_response(
                "Operator not found",
                404,
                f"No operator exists with ID '{operator_id}'. Cannot update non-existent operator."
            )
        
        # Sanitize input data
        data = sanitize_input(data)
        
        # Validation for updates
        validation_errors = validate_operator_data(data, update=True)
        if validation_errors:
            return create_error_response(
                "Validation failed",
                422,
                "Please fix the following issues with your update request",
                validation_errors
            )
        
        # Check if email is being changed to one that already exists
        if 'email' in data and data['email'] != operator.email:
            existing_operator = Operator.get_by_email(data['email'])
            if existing_operator:
                return create_error_response(
                    "Email address already in use",
                    409,
                    f"Another operator is already using email '{data['email']}'"
                )
        
        updated_operator = operator.update(data)
        return jsonify({
            "message": "Operator updated successfully",
            "data": updated_operator.to_dict()
        }), 200
        
    except Exception as e:
        current_app.logger.error(f"Error updating operator {operator_id}: {e}")
        return create_error_response(
            "Failed to update operator",
            500,
            "Please try again later or contact support",
            str(e)
        )

@operators_bp.route('/api/v1/operators/<operator_id>', methods=['DELETE'])
@require_auth
def delete_operator(operator_id):
    """Delete an operator"""
    # Validate operator ID format
    validation_error = validate_operator_id(operator_id)
    if validation_error:
        return create_error_response(
            validation_error,
            400,
            "Operator ID must be a valid UUID"
        )
    
    try:
        operator = Operator.get_by_id(operator_id)
        if not operator:
            return create_error_response(
                "Operator not found",
                404,
                f"No operator exists with ID '{operator_id}'. Cannot delete non-existent operator."
            )
        
        operator.delete()
        return jsonify({
            "message": "Operator deleted successfully"
        }), 200  # Changed from 204 to 200 to include confirmation message
        
    except Exception as e:
        current_app.logger.error(f"Error deleting operator {operator_id}: {e}")
        return create_error_response(
            "Failed to delete operator",
            500,
            "Please try again later or contact support",
            str(e)
        )

@operators_bp.route('/api/v1/operators/<operator_id>/skills', methods=['GET'])
def get_operator_skills(operator_id):
    """Get skills for a specific operator"""
    # Validate operator ID format
    validation_error = validate_operator_id(operator_id)
    if validation_error:
        return create_error_response(
            validation_error,
            400,
            "Operator ID must be a valid UUID"
        )
    
    try:
        operator = Operator.get_by_id(operator_id)
        if not operator:
            return create_error_response(
                "Operator not found",
                404,
                f"No operator exists with ID '{operator_id}'. Cannot retrieve skills for non-existent operator."
            )
        
        skills = operator.get_skills()
        return jsonify({
            "data": [skill.to_dict() for skill in skills] if skills else [],
            "count": len(skills),
            "operator_id": operator_id
        }), 200
        
    except Exception as e:
        current_app.logger.error(f"Error getting skills for operator {operator_id}: {e}")
        return create_error_response(
            "Failed to retrieve operator skills",
            500,
            "Please try again later or contact support",
            str(e)
        )
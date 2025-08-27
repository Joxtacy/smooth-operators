from flask import request, jsonify
import re
import jwt
import os
import uuid
from functools import wraps

def is_valid_uuid(uuid_string):
    """Check if a string is a valid UUID"""
    try:
        uuid.UUID(uuid_string)
        return True
    except (ValueError, TypeError):
        return False

def validate_operator_data(data, update=False):
    """Validate operator data with comprehensive validation"""
    errors = []
    
    # Name validation
    if not update and 'name' not in data:
        errors.append("name is required")
    elif 'name' in data:
        if not data['name'] or not isinstance(data['name'], str):
            errors.append("name must be a non-empty string")
        elif len(data['name'].strip()) == 0:
            errors.append("name cannot be empty or whitespace only")
        elif len(data['name'].strip()) > 255:
            errors.append("name cannot exceed 255 characters")
    
    # Email validation with better patterns and error messages
    if 'email' in data:
        if not data['email'] or not isinstance(data['email'], str):
            errors.append("email must be a non-empty string")
        elif len(data['email']) > 320:  # RFC 5321 limit
            errors.append("email cannot exceed 320 characters")
        elif not re.match(r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$', data['email']):
            errors.append("email format is invalid - must be a valid email address")
    elif not update:
        errors.append("email is required")
    
    # Phone validation with improved pattern and clearer error message
    if 'phone' in data and data['phone']:
        if not isinstance(data['phone'], str):
            errors.append("phone must be a string")
        elif len(data['phone']) > 20:
            errors.append("phone number cannot exceed 20 characters")
        elif not re.match(r'^\+?[1-9]\d{1,14}$', data['phone'].replace(' ', '').replace('-', '').replace('(', '').replace(')', '').replace('.', '')):
            errors.append("phone format is invalid - must be a valid phone number with optional country code")
    
    # Check for unexpected fields
    allowed_fields = {'name', 'email', 'phone'}
    unexpected_fields = set(data.keys()) - allowed_fields
    if unexpected_fields:
        errors.append(f"unexpected fields: {', '.join(unexpected_fields)}")
    
    return errors

def validate_operator_id(operator_id):
    """Validate operator ID format"""
    if not operator_id or not isinstance(operator_id, str):
        return "operator ID must be a non-empty string"
    
    if not is_valid_uuid(operator_id):
        return "operator ID must be a valid UUID format"
    
    return None

def sanitize_input(data):
    """Sanitize input data by trimming whitespace from string fields"""
    if isinstance(data, dict):
        sanitized = {}
        for key, value in data.items():
            if isinstance(value, str):
                sanitized[key] = value.strip()
            else:
                sanitized[key] = value
        return sanitized
    return data

def require_auth(f):
    """Decorator to require authentication with improved error handling"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        token = None
        
        # Check for token in Authorization header
        if 'Authorization' in request.headers:
            auth_header = request.headers['Authorization']
            if not auth_header.startswith('Bearer '):
                return jsonify({
                    "error": "Unauthorized: authorization header must start with 'Bearer '",
                    "hint": "Include 'Authorization: Bearer <token>' in your request headers"
                }), 401
            
            try:
                token = auth_header.split(" ", 1)[1]  # Get everything after "Bearer "
                if not token:
                    return jsonify({
                        "error": "Unauthorized: token is empty",
                        "hint": "Provide a valid JWT token after 'Bearer '"
                    }), 401
            except IndexError:
                return jsonify({
                    "error": "Unauthorized: invalid authorization header format",
                    "hint": "Use format 'Authorization: Bearer <token>'"
                }), 401
        else:
            return jsonify({
                "error": "Unauthorized: missing authorization header",
                "hint": "Include 'Authorization: Bearer <token>' in your request headers"
            }), 401
        
        try:
            # Decode JWT token
            secret = os.getenv('JWT_SECRET', 'default-secret-key')
            if secret == 'default-secret-key':
                # Log warning about using default secret in production
                import logging
                logging.warning("Using default JWT secret key - this is insecure for production!")
            
            payload = jwt.decode(token, secret, algorithms=['HS256'])
            user_id = payload.get('user_id')
            
            if not user_id:
                return jsonify({
                    "error": "Unauthorized: token does not contain user_id",
                    "hint": "Token must include a valid user_id claim"
                }), 401
                
            request.user_id = user_id
            
        except jwt.ExpiredSignatureError:
            return jsonify({
                "error": "Unauthorized: token has expired",
                "hint": "Please obtain a new authentication token"
            }), 401
        except jwt.InvalidTokenError as e:
            return jsonify({
                "error": "Unauthorized: invalid token",
                "hint": "Please provide a valid JWT token",
                "details": str(e) if os.getenv('DEBUG') else None
            }), 401
        
        return f(*args, **kwargs)
    return decorated_function

def create_error_response(message, status_code, hint=None, details=None):
    """Create standardized error responses"""
    response = {"error": message}
    
    if hint:
        response["hint"] = hint
    
    if details and os.getenv('DEBUG'):
        response["details"] = details
    
    return jsonify(response), status_code
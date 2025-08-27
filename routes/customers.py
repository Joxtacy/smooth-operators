from flask import Blueprint, request, jsonify
from functools import wraps
import re

customers_bp = Blueprint('customers', __name__)

# Mock database
customers_db = [
    {"id": "CUST-001", "name": "John Doe", "email": "john@example.com", "phone": "+1234567890"},
    {"id": "CUST-002", "name": "Jane Smith", "email": "jane@example.com", "phone": "+0987654321"}
]

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        if 'Authorization' not in request.headers:
            return jsonify({
                'error': 'Authentication required',
                'message': 'Please provide a valid Bearer token in the Authorization header',
                'code': 'MISSING_AUTH_TOKEN'
            }), 401
        return f(*args, **kwargs)
    return decorated

@customers_bp.route('/customers', methods=['POST'])
@token_required
def create_customer():
    data = request.get_json()
    
    if not data:
        return jsonify({
            'error': 'No data provided',
            'message': 'Request body must contain valid JSON data',
            'code': 'MISSING_REQUEST_BODY'
        }), 400
    
    # Check for missing or empty required fields
    required_fields = ['name', 'email', 'phone']
    missing_fields = []
    empty_fields = []
    
    for field in required_fields:
        if field not in data:
            missing_fields.append(field)
        elif not data[field] or not str(data[field]).strip():
            empty_fields.append(field)
    
    if missing_fields or empty_fields:
        error_details = []
        if missing_fields:
            error_details.append(f'Missing fields: {", ".join(missing_fields)}')
        if empty_fields:
            error_details.append(f'Empty fields: {", ".join(empty_fields)}')
        
        return jsonify({
            'error': 'Required field validation failed',
            'message': f'{" | ".join(error_details)}. All required fields must be provided and non-empty.',
            'required_fields': required_fields,
            'missing_fields': missing_fields,
            'empty_fields': empty_fields,
            'field_descriptions': {
                'name': 'Customer full name (string)',
                'email': 'Valid email address (user@domain.com)',
                'phone': 'Phone number with country code (+1234567890)'
            },
            'code': 'REQUIRED_FIELD_VALIDATION_FAILED'
        }), 400
    
    # Validate email format
    email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(email_pattern, data['email']):
        return jsonify({
            'error': 'Invalid email format',
            'message': 'email must be a valid email address format',
            'provided_value': data.get('email'),
            'expected_format': 'user@domain.com',
            'pattern_requirements': [
                'Must contain @ symbol',
                'Must have domain with at least one dot',
                'Domain extension must be at least 2 characters',
                'No spaces allowed'
            ],
            'example_values': ['user@example.com', 'john.doe@company.co.uk', 'admin@test.org'],
            'code': 'INVALID_EMAIL_FORMAT'
        }), 400
    
    # Check for duplicate email
    existing_customer = next((c for c in customers_db if c['email'].lower() == data['email'].lower()), None)
    if existing_customer:
        return jsonify({
            'error': 'Email already exists',
            'message': f'A customer with email "{data["email"]}" already exists. Each customer must have a unique email address.',
            'provided_email': data['email'],
            'existing_customer_id': existing_customer['id'],
            'existing_customer_name': existing_customer['name'],
            'suggested_actions': [
                'Use a different email address',
                'Update the existing customer instead',
                f'Retrieve existing customer using GET /customers/{existing_customer["id"]}'
            ],
            'code': 'EMAIL_ALREADY_EXISTS'
        }), 409
    
    # Validate phone format (basic validation)
    phone_pattern = r'^\+?[1-9]\d{1,14}$'
    if not re.match(phone_pattern, data['phone']):
        return jsonify({
            'error': 'Invalid phone number format',
            'message': 'phone must be a valid international phone number format',
            'provided_value': data.get('phone'),
            'expected_format': '+[country_code][number] or [country_code][number]',
            'pattern_requirements': [
                'Must start with + (optional) followed by country code',
                'Country code cannot start with 0',
                'Total length between 2-15 digits after country code',
                'Only digits allowed (no spaces, dashes, or parentheses)'
            ],
            'example_values': ['+1234567890', '1234567890', '+441234567890', '441234567890'],
            'code': 'INVALID_PHONE_FORMAT'
        }), 400
    
    # Generate customer ID
    customer_id = f"CUST-{len(customers_db) + 1:03d}"
    
    new_customer = {
        'id': customer_id,
        'name': data['name'].strip(),
        'email': data['email'].lower(),
        'phone': data['phone']
    }
    
    customers_db.append(new_customer)
    return jsonify(new_customer), 201

@customers_bp.route('/customers/<customer_id>', methods=['PUT'])
@token_required
def update_customer(customer_id):
    customer = next((c for c in customers_db if c['id'] == customer_id), None)
    if not customer:
        return jsonify({
            'error': 'Customer not found',
            'message': f'No customer exists with ID "{customer_id}". Please check the customer ID and try again.',
            'requested_id': customer_id,
            'available_customers': [c['id'] for c in customers_db],
            'id_format': 'CUST-XXX (e.g., CUST-001)',
            'code': 'CUSTOMER_NOT_FOUND'
        }), 404
    
    data = request.get_json()
    if not data:
        return jsonify({
            'error': 'No update data provided',
            'message': 'Request body must contain JSON data with fields to update',
            'updateable_fields': ['name', 'email', 'phone'],
            'current_values': {
                'name': customer['name'],
                'email': customer['email'],
                'phone': customer['phone']
            },
            'code': 'MISSING_UPDATE_DATA'
        }), 400
    
    # Validate email format if provided
    if 'email' in data:
        if not data['email'] or not data['email'].strip():
            return jsonify({
                'error': 'Invalid email value for update',
                'message': 'email cannot be empty or contain only whitespace',
                'provided_value': data['email'],
                'current_value': customer['email'],
                'code': 'INVALID_EMAIL_UPDATE_VALUE'
            }), 400
        
        email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
        if not re.match(email_pattern, data['email']):
            return jsonify({
                'error': 'Invalid email format for update',
                'message': 'email must be a valid email address format',
                'provided_value': data['email'],
                'current_value': customer['email'],
                'expected_format': 'user@domain.com',
                'code': 'INVALID_EMAIL_UPDATE_FORMAT'
            }), 400
        
        # Check for duplicate email (excluding current customer)
        existing_customer = next((c for c in customers_db if c['email'].lower() == data['email'].lower() and c['id'] != customer_id), None)
        if existing_customer:
            return jsonify({
                'error': 'Email already exists for update',
                'message': f'Cannot update email to "{data["email"]}" because another customer already uses this email address.',
                'provided_email': data['email'],
                'current_email': customer['email'],
                'conflicting_customer_id': existing_customer['id'],
                'conflicting_customer_name': existing_customer['name'],
                'suggested_actions': [
                    'Use a different email address',
                    'Check if you meant to update the other customer instead'
                ],
                'code': 'EMAIL_ALREADY_EXISTS_UPDATE'
            }), 409
        
        customer['email'] = data['email'].lower()
    
    # Validate phone format if provided
    if 'phone' in data:
        if not data['phone'] or not str(data['phone']).strip():
            return jsonify({
                'error': 'Invalid phone value for update',
                'message': 'phone cannot be empty or contain only whitespace',
                'provided_value': data['phone'],
                'current_value': customer['phone'],
                'code': 'INVALID_PHONE_UPDATE_VALUE'
            }), 400
        
        phone_pattern = r'^\+?[1-9]\d{1,14}$'
        if not re.match(phone_pattern, data['phone']):
            return jsonify({
                'error': 'Invalid phone format for update',
                'message': 'phone must be a valid international phone number format',
                'provided_value': data['phone'],
                'current_value': customer['phone'],
                'expected_format': '+[country_code][number] or [country_code][number]',
                'code': 'INVALID_PHONE_UPDATE_FORMAT'
            }), 400
        customer['phone'] = data['phone']
    
    # Update name if provided
    if 'name' in data:
        if not data['name'] or not data['name'].strip():
            return jsonify({
                'error': 'Invalid name value for update',
                'message': 'name cannot be empty or contain only whitespace',
                'provided_value': data['name'],
                'current_value': customer['name'],
                'code': 'INVALID_NAME_UPDATE_VALUE'
            }), 400
        customer['name'] = data['name'].strip()
    
    return jsonify(customer)
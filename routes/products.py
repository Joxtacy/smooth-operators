from flask import Blueprint, request, jsonify
from functools import wraps
import re

products_bp = Blueprint('products', __name__)

# Mock database
products_db = [
    {"id": "PRD-001", "name": "Laptop", "price": 999.99, "category": "Electronics", "stock": 50, "description": "High-performance laptop"},
    {"id": "PRD-002", "name": "Mouse", "price": 29.99, "category": "Electronics", "stock": 100, "description": "Wireless mouse"}
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

@products_bp.route('/products', methods=['POST'])
@token_required
def create_product():
    data = request.get_json()
    
    if not data:
        return jsonify({
            'error': 'No data provided',
            'message': 'Request body must contain valid JSON data',
            'code': 'MISSING_REQUEST_BODY'
        }), 400
    
    # Check for missing or empty required fields
    required_fields = ['name', 'price', 'category', 'stock']
    missing_fields = []
    empty_fields = []
    
    for field in required_fields:
        if field not in data:
            missing_fields.append(field)
        elif not data[field] and data[field] != 0:  # Allow 0 as valid value for numeric fields
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
                'name': 'Product name (string)',
                'price': 'Product price (positive number)',
                'category': 'Product category (string)',
                'stock': 'Stock quantity (non-negative integer)'
            },
            'code': 'REQUIRED_FIELD_VALIDATION_FAILED'
        }), 400
    
    # Validate price
    try:
        price = float(data['price'])
        if price <= 0:
            return jsonify({
                'error': 'Invalid price value',
                'message': 'price must be a positive number greater than 0',
                'provided_value': data['price'],
                'valid_range': 'positive numbers > 0',
                'example_values': [9.99, 100.50, 1299.95],
                'code': 'INVALID_PRICE_VALUE'
            }), 422
    except (ValueError, TypeError):
        return jsonify({
            'error': 'Invalid price type',
            'message': 'price must be a valid number (integer or decimal)',
            'provided_value': data.get('price'),
            'provided_type': str(type(data.get('price')).__name__),
            'expected_type': 'number (int or float)',
            'example_values': [9.99, 100, 1299.95],
            'code': 'INVALID_PRICE_TYPE'
        }), 400
    
    # Validate stock
    try:
        stock = int(data['stock'])
        if stock < 0:
            return jsonify({
                'error': 'Invalid stock value',
                'message': 'stock must be a non-negative integer (0 or greater)',
                'provided_value': data['stock'],
                'valid_range': 'non-negative integers >= 0',
                'example_values': [0, 10, 100, 500],
                'code': 'INVALID_STOCK_VALUE'
            }), 422
    except (ValueError, TypeError):
        return jsonify({
            'error': 'Invalid stock type',
            'message': 'stock must be a valid integer number',
            'provided_value': data.get('stock'),
            'provided_type': str(type(data.get('stock')).__name__),
            'expected_type': 'integer',
            'example_values': [0, 10, 100, 500],
            'code': 'INVALID_STOCK_TYPE'
        }), 400
    
    # Generate product ID
    product_id = f"PRD-{len(products_db) + 1:03d}"
    
    new_product = {
        'id': product_id,
        'name': data['name'],
        'price': price,
        'category': data['category'],
        'stock': stock,
        'description': data.get('description', '')
    }
    
    products_db.append(new_product)
    return jsonify(new_product), 201

@products_bp.route('/products/<product_id>', methods=['PATCH'])
@token_required
def update_product(product_id):
    product = next((p for p in products_db if p['id'] == product_id), None)
    if not product:
        return jsonify({
            'error': 'Product not found',
            'message': f'No product exists with ID "{product_id}". Please check the product ID and try again.',
            'requested_id': product_id,
            'available_products': [p['id'] for p in products_db],
            'id_format': 'PRD-XXX (e.g., PRD-001)',
            'code': 'PRODUCT_NOT_FOUND'
        }), 404
    
    data = request.get_json()
    if not data:
        return jsonify({
            'error': 'No update data provided',
            'message': 'Request body must contain JSON data with fields to update',
            'updateable_fields': ['name', 'price', 'category', 'stock', 'description'],
            'code': 'MISSING_UPDATE_DATA'
        }), 400
    
    # Validate price if provided
    if 'price' in data:
        try:
            price = float(data['price'])
            if price <= 0:
                return jsonify({
                    'error': 'Invalid price value for update',
                    'message': 'price must be a positive number greater than 0',
                    'provided_value': data['price'],
                    'current_value': product['price'],
                    'valid_range': 'positive numbers > 0',
                    'code': 'INVALID_PRICE_UPDATE_VALUE'
                }), 422
            product['price'] = price
        except (ValueError, TypeError):
            return jsonify({
                'error': 'Invalid price type for update',
                'message': 'price must be a valid number (integer or decimal)',
                'provided_value': data.get('price'),
                'provided_type': str(type(data.get('price')).__name__),
                'expected_type': 'number (int or float)',
                'current_value': product['price'],
                'code': 'INVALID_PRICE_UPDATE_TYPE'
            }), 400
    
    # Validate stock if provided
    if 'stock' in data:
        try:
            stock = int(data['stock'])
            if stock < 0:
                return jsonify({
                    'error': 'Invalid stock value for update',
                    'message': 'stock must be a non-negative integer (0 or greater)',
                    'provided_value': data['stock'],
                    'current_value': product['stock'],
                    'valid_range': 'non-negative integers >= 0',
                    'code': 'INVALID_STOCK_UPDATE_VALUE'
                }), 422
            product['stock'] = stock
        except (ValueError, TypeError):
            return jsonify({
                'error': 'Invalid stock type for update',
                'message': 'stock must be a valid integer number',
                'provided_value': data.get('stock'),
                'provided_type': str(type(data.get('stock')).__name__),
                'expected_type': 'integer',
                'current_value': product['stock'],
                'code': 'INVALID_STOCK_UPDATE_TYPE'
            }), 400
    
    # Update other fields
    if 'name' in data:
        if not data['name'] or not data['name'].strip():
            return jsonify({
                'error': 'Invalid name value for update',
                'message': 'name cannot be empty or contain only whitespace',
                'provided_value': data['name'],
                'current_value': product['name'],
                'code': 'INVALID_NAME_UPDATE_VALUE'
            }), 400
        product['name'] = data['name'].strip()
    
    if 'category' in data:
        if not data['category'] or not data['category'].strip():
            return jsonify({
                'error': 'Invalid category value for update',
                'message': 'category cannot be empty or contain only whitespace',
                'provided_value': data['category'],
                'current_value': product['category'],
                'code': 'INVALID_CATEGORY_UPDATE_VALUE'
            }), 400
        product['category'] = data['category'].strip()
    
    if 'description' in data:
        product['description'] = data['description']
    
    return jsonify(product)

@products_bp.route('/products/search', methods=['GET'])
@token_required
def search_products():
    query = request.args.get('q', '').strip()
    category = request.args.get('category', '').strip()
    
    if not query and not category:
        return jsonify({
            'error': 'Missing search parameters',
            'message': 'At least one search parameter (q or category) must be provided and non-empty',
            'provided_parameters': {
                'q': query if query else 'empty or missing',
                'category': category if category else 'empty or missing'
            },
            'available_parameters': {
                'q': 'Search query for product name (string)',
                'category': 'Filter by product category (string)'
            },
            'example_urls': [
                '/products/search?q=laptop',
                '/products/search?category=Electronics',
                '/products/search?q=mouse&category=Electronics'
            ],
            'code': 'MISSING_SEARCH_PARAMETERS'
        }), 400
    
    # Simple search implementation
    results = []
    for product in products_db:
        if query and query.lower() in product['name'].lower():
            results.append(product)
        elif category and category.lower() == product['category'].lower():
            results.append(product)
    
    return jsonify({
        'products': results,
        'search_parameters': {
            'query': query if query else None,
            'category': category if category else None
        },
        'total_results': len(results)
    })
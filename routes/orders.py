from flask import Blueprint, request, jsonify
from functools import wraps
import re
from datetime import datetime
import json

orders_bp = Blueprint('orders', __name__)

# Mock database
orders_db = [
    {"id": 1, "product_id": "PRD-001", "quantity": 2, "customer_email": "john@example.com", "status": "pending", "created_at": "2024-01-01T10:00:00Z"},
    {"id": 2, "product_id": "PRD-002", "quantity": 1, "customer_email": "jane@example.com", "status": "completed", "created_at": "2024-01-02T11:00:00Z"}
]

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        # Simple token validation - in production use proper JWT validation
        if 'Authorization' not in request.headers:
            return jsonify({
                'error': 'Authentication required',
                'message': 'Please provide a valid Bearer token in the Authorization header',
                'code': 'MISSING_AUTH_TOKEN'
            }), 401
        return f(*args, **kwargs)
    return decorated

@orders_bp.route('/orders', methods=['POST'])
@token_required
def create_order():
    data = request.get_json()
    
    if not data:
        return jsonify({
            'error': 'No data provided',
            'message': 'Request body must contain valid JSON data',
            'code': 'MISSING_REQUEST_BODY'
        }), 400
    
    # Check for missing required fields
    missing_fields = []
    required_fields = ['product_id', 'quantity', 'customer_email']
    for field in required_fields:
        if field not in data or not data[field]:
            missing_fields.append(field)
    
    if missing_fields:
        return jsonify({
            'error': 'Missing required fields',
            'message': f'The following required fields are missing or empty: {", ".join(missing_fields)}',
            'required_fields': required_fields,
            'missing_fields': missing_fields,
            'code': 'MISSING_REQUIRED_FIELDS'
        }), 400
    
    # Validate product_id format
    if not isinstance(data['product_id'], str) or not data['product_id'].startswith('PRD-'):
        return jsonify({
            'error': 'Invalid product_id format',
            'message': 'product_id must be a string starting with "PRD-" followed by numbers (e.g., "PRD-001")',
            'provided_value': data.get('product_id'),
            'expected_format': 'PRD-XXX (where XXX is a number)',
            'code': 'INVALID_PRODUCT_ID_FORMAT'
        }), 400
    
    # Validate quantity
    try:
        quantity = int(data['quantity'])
        if quantity <= 0:
            return jsonify({
                'error': 'Invalid quantity value',
                'message': 'quantity must be a positive integer greater than 0',
                'provided_value': data['quantity'],
                'valid_range': 'positive integers > 0',
                'code': 'INVALID_QUANTITY_VALUE'
            }), 422
    except (ValueError, TypeError):
        return jsonify({
            'error': 'Invalid quantity type',
            'message': 'quantity must be a valid integer number',
            'provided_value': data.get('quantity'),
            'provided_type': str(type(data.get('quantity')).__name__),
            'expected_type': 'integer',
            'code': 'INVALID_QUANTITY_TYPE'
        }), 400
    
    # Validate email format
    email_pattern = r'^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$'
    if not re.match(email_pattern, data['customer_email']):
        return jsonify({
            'error': 'Invalid email format',
            'message': 'customer_email must be a valid email address (e.g., user@example.com)',
            'provided_value': data.get('customer_email'),
            'expected_format': 'user@domain.com',
            'code': 'INVALID_EMAIL_FORMAT'
        }), 400
    
    # Create new order
    new_order = {
        'id': len(orders_db) + 1,
        'product_id': data['product_id'],
        'quantity': quantity,
        'customer_email': data['customer_email'],
        'status': 'pending',
        'created_at': datetime.now().isoformat() + 'Z'
    }
    
    orders_db.append(new_order)
    return jsonify(new_order), 201

@orders_bp.route('/orders/<int:order_id>', methods=['GET'])
@token_required
def get_order(order_id):
    order = next((o for o in orders_db if o['id'] == order_id), None)
    if not order:
        return jsonify({
            'error': 'Order not found',
            'message': f'No order exists with ID {order_id}. Please check the order ID and try again.',
            'requested_id': order_id,
            'available_orders': [o['id'] for o in orders_db],
            'code': 'ORDER_NOT_FOUND'
        }), 404
    return jsonify(order)

@orders_bp.route('/orders/<int:order_id>', methods=['PUT'])
@token_required
def update_order(order_id):
    order = next((o for o in orders_db if o['id'] == order_id), None)
    if not order:
        return jsonify({
            'error': 'Order not found',
            'message': f'Cannot update order with ID {order_id} because it does not exist.',
            'requested_id': order_id,
            'code': 'ORDER_NOT_FOUND'
        }), 404
    
    data = request.get_json()
    if not data:
        return jsonify({
            'error': 'No update data provided',
            'message': 'Request body must contain JSON data with fields to update',
            'updateable_fields': ['status', 'notes'],
            'code': 'MISSING_UPDATE_DATA'
        }), 400
    
    # Validate status if provided
    valid_statuses = ['pending', 'processing', 'shipped', 'delivered', 'cancelled']
    if 'status' in data and data['status'] not in valid_statuses:
        return jsonify({
            'error': 'Invalid order status',
            'message': 'status must be one of the allowed values',
            'provided_value': data['status'],
            'valid_statuses': valid_statuses,
            'code': 'INVALID_ORDER_STATUS'
        }), 400
    
    # Update order
    if 'status' in data:
        order['status'] = data['status']
    if 'notes' in data:
        order['notes'] = data['notes']
    
    return jsonify(order)

@orders_bp.route('/orders/<int:order_id>', methods=['DELETE'])
@token_required
def delete_order(order_id):
    order = next((o for o in orders_db if o['id'] == order_id), None)
    if not order:
        return jsonify({
            'error': 'Order not found',
            'message': f'Cannot delete order with ID {order_id} because it does not exist.',
            'requested_id': order_id,
            'code': 'ORDER_NOT_FOUND'
        }), 404
    
    # Check if order can be deleted (only pending orders)
    if order['status'] != 'pending':
        return jsonify({
            'error': 'Order deletion not allowed',
            'message': f'Cannot delete order with status "{order["status"]}". Only orders with status "pending" can be deleted.',
            'current_status': order['status'],
            'deletable_statuses': ['pending'],
            'suggested_action': 'Cancel the order instead of deleting it',
            'code': 'ORDER_DELETION_FORBIDDEN'
        }), 403
    
    orders_db.remove(order)
    return jsonify({'message': 'Order deleted successfully'})

@orders_bp.route('/orders', methods=['GET'])
@token_required
def list_orders():
    # Get pagination parameters
    try:
        limit = int(request.args.get('limit', 10))
        offset = int(request.args.get('offset', 0))
    except ValueError:
        return jsonify({
            'error': 'Invalid pagination parameters',
            'message': 'limit and offset must be valid integer numbers',
            'provided_limit': request.args.get('limit'),
            'provided_offset': request.args.get('offset'),
            'expected_types': 'integers',
            'example': '/orders?limit=10&offset=0',
            'code': 'INVALID_PAGINATION_PARAMS'
        }), 400
    
    if limit <= 0:
        return jsonify({
            'error': 'Invalid limit value',
            'message': 'limit must be a positive integer greater than 0',
            'provided_value': limit,
            'valid_range': '1 to 1000',
            'code': 'INVALID_LIMIT_VALUE'
        }), 400
    
    if offset < 0:
        return jsonify({
            'error': 'Invalid offset value',
            'message': 'offset must be a non-negative integer (0 or greater)',
            'provided_value': offset,
            'valid_range': '0 or greater',
            'code': 'INVALID_OFFSET_VALUE'
        }), 400
    
    # Apply pagination
    paginated_orders = orders_db[offset:offset+limit]
    return jsonify({
        'orders': paginated_orders,
        'total': len(orders_db),
        'limit': limit,
        'offset': offset
    })

@orders_bp.route('/orders/search', methods=['GET'])
@token_required
def search_orders():
    date_from = request.args.get('date_from')
    date_to = request.args.get('date_to')
    
    if date_from:
        try:
            datetime.strptime(date_from, '%Y-%m-%d')
        except ValueError:
            return jsonify({
                'error': 'Invalid date_from format',
                'message': 'date_from must be in YYYY-MM-DD format',
                'provided_value': date_from,
                'expected_format': 'YYYY-MM-DD',
                'example': '2024-12-31',
                'code': 'INVALID_DATE_FROM_FORMAT'
            }), 400
    
    if date_to:
        try:
            datetime.strptime(date_to, '%Y-%m-%d')
        except ValueError:
            return jsonify({
                'error': 'Invalid date_to format',
                'message': 'date_to must be in YYYY-MM-DD format',
                'provided_value': date_to,
                'expected_format': 'YYYY-MM-DD',
                'example': '2024-12-31',
                'code': 'INVALID_DATE_TO_FORMAT'
            }), 400
    
    # For now, just return all orders (in real implementation, filter by date)
    return jsonify({'orders': orders_db})
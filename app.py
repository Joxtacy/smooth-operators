from flask import Flask, request, jsonify
from functools import wraps
import re
import jwt
from datetime import datetime
import os
from routes.orders import orders_bp
from routes.customers import customers_bp
from routes.products import products_bp

app = Flask(__name__)
app.secret_key = os.getenv('SECRET_KEY', 'dev-secret-key')

# Register blueprints
app.register_blueprint(orders_bp, url_prefix='/api/v1')
app.register_blueprint(customers_bp, url_prefix='/api/v1')
app.register_blueprint(products_bp, url_prefix='/api/v1')

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None
        
        if 'Authorization' in request.headers:
            auth_header = request.headers['Authorization']
            try:
                token = auth_header.split(" ")[1]
            except IndexError:
                return jsonify({
                    'error': 'Invalid Authorization header format', 
                    'message': 'Authorization header must be in format: Bearer <token>',
                    'code': 'INVALID_AUTH_HEADER'
                }), 401
        
        if not token:
            return jsonify({
                'error': 'Missing Authorization token', 
                'message': 'Please provide a valid Bearer token in the Authorization header',
                'code': 'MISSING_TOKEN'
            }), 401
        
        try:
            data = jwt.decode(token, app.secret_key, algorithms=['HS256'])
            current_user = data.get('user_id')
        except jwt.ExpiredSignatureError:
            return jsonify({
                'error': 'Token expired', 
                'message': 'Your authentication token has expired. Please obtain a new token.',
                'code': 'TOKEN_EXPIRED'
            }), 401
        except jwt.InvalidTokenError:
            return jsonify({
                'error': 'Invalid token', 
                'message': 'The provided authentication token is invalid or malformed.',
                'code': 'INVALID_TOKEN'
            }), 401
        
        return f(current_user, *args, **kwargs)
    return decorated

@app.errorhandler(400)
def bad_request(error):
    return jsonify({
        'error': 'Bad Request',
        'message': 'The request could not be processed due to invalid syntax or missing required data.',
        'code': 'BAD_REQUEST'
    }), 400

@app.errorhandler(404)
def not_found(error):
    return jsonify({
        'error': 'Resource not found',
        'message': 'The requested resource does not exist. Please check the URL and try again.',
        'code': 'NOT_FOUND'
    }), 404

@app.errorhandler(422)
def validation_error(error):
    return jsonify({
        'error': 'Validation failed',
        'message': 'The request data failed validation. Please check the provided values.',
        'code': 'VALIDATION_ERROR'
    }), 422

@app.errorhandler(409)
def conflict_error(error):
    return jsonify({
        'error': 'Resource conflict',
        'message': 'The request conflicts with the current state of the resource.',
        'code': 'RESOURCE_CONFLICT'
    }), 409

@app.errorhandler(403)
def forbidden_error(error):
    return jsonify({
        'error': 'Access forbidden',
        'message': 'You do not have permission to perform this action.',
        'code': 'ACCESS_FORBIDDEN'
    }), 403

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80, debug=False)